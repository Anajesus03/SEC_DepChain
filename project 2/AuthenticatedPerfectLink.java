import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

public class AuthenticatedPerfectLink {

    private networkClass networkClass;
    private cryptoClass crypto;
    public int sequenceNumbers; // AP2: No duplication
    private Set<Integer> receivedSequenceNumbers = new HashSet<>(); 
    private static final int MAX_UDP_SIZE = 65507; // Maximum size of a UDP packet
    private int nodeId;

    public AuthenticatedPerfectLink(networkClass networkClass, cryptoClass crypto, int nodeId) {
        this.networkClass = networkClass;
        this.crypto = crypto;
        this.sequenceNumbers = (nodeId+1) * 1000; // AP2: No duplication
        this.nodeId = nodeId;
    }

    // send message with authentication
    public void sendMessage(String message, InetAddress dest, int port) throws Exception {
        //System.out.println("[Sender Node " + nodeId +"] Preparing message: " + message);
        byte[] data = message.getBytes();
        byte[] sequenceNumberBytes = ByteBuffer.allocate(4).putInt(sequenceNumbers).array();
        byte[] dataWithSequenceNumber = new byte[data.length + sequenceNumberBytes.length];
        System.arraycopy(sequenceNumberBytes, 0, dataWithSequenceNumber, 0, sequenceNumberBytes.length);
        System.arraycopy(data, 0, dataWithSequenceNumber, sequenceNumberBytes.length, data.length);

        // Final message structure: [sequenceNumber | message | publicKey | signature]
        //System.out.println("[Sender Node " + nodeId +"]  Signing message...");
        byte[] signature = crypto.signMessage(dataWithSequenceNumber);
        byte[] publicKeyBytes = crypto.getPublicKey().getEncoded();
        ByteBuffer buffer = ByteBuffer.allocate(
        dataWithSequenceNumber.length + publicKeyBytes.length + signature.length
        );
        buffer.put(dataWithSequenceNumber);
        buffer.put(publicKeyBytes);
        buffer.put(signature);
    
        byte[] finalMessage = buffer.array();

        boolean receivedACK = false; // AP1: Reliable delivery

        if (finalMessage.length > MAX_UDP_SIZE) {
            throw new Exception("Message too large");
        }

        while(!receivedACK) {
            //System.out.println("[Sender Node " + nodeId +"] Sending message to " + dest.getHostAddress() + ":" + port);
            networkClass.sendPacket(dest, port, finalMessage);
            try {
                //System.out.println("[Sender Node " + nodeId +"] Waiting for ACK...");
                DatagramPacket ackPacket = receiveACKWithTimeout(40000);
                if(ackPacket != null) {
                    receivedACK = true;
                    System.out.println("[Sender Node " + nodeId +"] Received ACK from " + ackPacket.getAddress().getHostAddress() + ":" + ackPacket.getPort());
                }
            } catch (Exception e) {
                System.out.println("Timeout, resending message");
            }
        }

        sequenceNumbers++;
        System.out.println("[Sender Node " + nodeId +"] Message sent successfully.");
    }

    // receive message with authentication

    public String receiveMessage() throws Exception {
        System.out.println("[Receiver Node " + nodeId +"] Waiting for packet...");
        DatagramPacket packet = networkClass.receivePacket();
        System.out.println("[Receiver Node " + nodeId +"] Received packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

        byte[] receivedData = packet.getData();
        int packetLength = packet.getLength();

        int publicKeyLength = crypto.getPublicKeyLength();
        int signatureLength = crypto.getSignatureLength();
        int messageLength = packetLength - (publicKeyLength + signatureLength);

        if (messageLength < 4) {
            throw new Exception("[Receiver Node " + nodeId +"] Message too short!");
        }

        // Extract payload
        byte[] payload = new byte[messageLength];
        System.arraycopy(receivedData, 0, payload, 0, messageLength);

        // Extract public key
        byte[] publicKeyBytes = new byte[publicKeyLength];
        System.arraycopy(receivedData, messageLength, publicKeyBytes, 0, publicKeyLength);
        PublicKey senderPublicKey = crypto.getPublicKeyFromBytes(publicKeyBytes);

        // Extract signature
        byte[] signature = new byte[signatureLength];
        System.arraycopy(receivedData, messageLength + publicKeyLength, signature, 0, signatureLength);

        //System.out.println("[Receiver Node " + nodeId +"] Verifying signature...");
        if (crypto.verifySignature(payload, signature, senderPublicKey)) {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            int sequenceNumber = buffer.getInt();
            byte[] message = new byte[payload.length - 4];
            buffer.get(message);

            System.out.println("[Receiver Node " + nodeId +"] Signature verified.");
            System.out.println("[Receiver Node " + nodeId +"] Checking for duplicate message...");
            

            if (!receivedSequenceNumbers.contains(sequenceNumber)) {
                receivedSequenceNumbers.add(sequenceNumber);
                //System.out.println("[Receiver Node " + nodeId +"] Sending ACK...");
                sendACK(packet.getAddress(), packet.getPort());
                //System.out.println("[Receiver Node " + nodeId +"]Processing message: " + new String(message));
                return new String(message);
            } else {
                throw new Exception("[Receiver Node " + nodeId +"] Duplicate message detected!");
            }
        } else {
            throw new Exception("[Receiver Node " + nodeId +"] Invalid signature!");
        }
    }

    // function to send ACK messages
    private void sendACK(InetAddress address, int port) throws Exception {
        //System.out.println("[Receiver Node" + nodeId +"] Sending ACK to " + address.getHostAddress() + ":" + port);
        byte[] ack = "ACK".getBytes();
        networkClass.sendPacket(address, port, ack);
    }

    // function to receive ACK messages with timeout
    private DatagramPacket receiveACKWithTimeout(int timeout) throws Exception {
        //System.out.println("[Sender Node " + nodeId +"] Waiting for ACK with timeout " + timeout + "ms...");
        byte[] aux = new byte[4];
        DatagramPacket ackPacket = new DatagramPacket(aux, aux.length);
        networkClass.getSocket().setSoTimeout(timeout);

        try {
            networkClass.getSocket().receive(ackPacket);
            if (new String(ackPacket.getData(),0, ackPacket.getLength()).equals("ACK")) {
                //System.out.println("[Sender Node " + nodeId +"] ACK received.");
                return ackPacket;
            }
        } catch (SocketTimeoutException e) {
            return null;
        }

        return null; 
    }

    public void close() {
        networkClass.closeSocket();
    }

    public DatagramSocket getSocket() {
        return networkClass.getSocket();
    }
    
    public cryptoClass getCrypto() {
        return crypto;
    }

    public int getNodeId() {
        return nodeId;
    }
}
