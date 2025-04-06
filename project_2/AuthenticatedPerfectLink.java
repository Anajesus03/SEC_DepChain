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
        byte[] finalMessage = prepareFinalMessage(message);
    
        if (finalMessage.length > MAX_UDP_SIZE) {
            throw new Exception("Message too large");
        }
    
        sendWithRetry(finalMessage, dest, port);
        sequenceNumbers++;
        System.out.println("[Sender Node " + nodeId + "] Message sent successfully.");
    }
    
    private byte[] prepareFinalMessage(String message) throws Exception {
        byte[] data = message.getBytes();
        byte[] sequenceNumberBytes = ByteBuffer.allocate(4).putInt(sequenceNumbers).array();
    
        byte[] dataWithSequenceNumber = new byte[data.length + sequenceNumberBytes.length];
        System.arraycopy(sequenceNumberBytes, 0, dataWithSequenceNumber, 0, sequenceNumberBytes.length);
        System.arraycopy(data, 0, dataWithSequenceNumber, sequenceNumberBytes.length, data.length);
    
        byte[] signature = crypto.signMessage(dataWithSequenceNumber);
        byte[] publicKeyBytes = crypto.getPublicKey().getEncoded();
    
        ByteBuffer buffer = ByteBuffer.allocate(
            dataWithSequenceNumber.length + publicKeyBytes.length + signature.length
        );
        buffer.put(dataWithSequenceNumber);
        buffer.put(publicKeyBytes);
        buffer.put(signature);
    
        return buffer.array();
    }
    
    private void sendWithRetry(byte[] message, InetAddress dest, int port) throws Exception {
        boolean receivedACK = false;
    
        while (!receivedACK) {
            networkClass.sendPacket(dest, port, message); // now allowed
            try {
                DatagramPacket ackPacket = receiveACKWithTimeout(40000);
                if (ackPacket != null) {
                    receivedACK = true;
                    System.out.println("[Sender Node " + nodeId + "] Received ACK from "
                            + ackPacket.getAddress().getHostAddress() + ":" + ackPacket.getPort());
                }
            } catch (Exception e) {
                System.out.println("Timeout, resending message");
            }
        }
    }
    
    public String receiveMessage() throws Exception {
        DatagramPacket packet = receivePacketWithLogging();
    
        byte[] receivedData = packet.getData();
        int packetLength = packet.getLength();
    
        int publicKeyLength = crypto.getPublicKeyLength();
        int signatureLength = crypto.getSignatureLength();
        int messageLength = packetLength - (publicKeyLength + signatureLength);
    
        if (messageLength < 4) {
            throw new Exception("[Receiver Node " + nodeId + "] Message too short!");
        }
    
        // Extract components
        byte[] payload = extractPayload(receivedData, messageLength);
        byte[] publicKeyBytes = extractPublicKey(receivedData, messageLength, publicKeyLength);
        byte[] signature = extractSignature(receivedData, messageLength, publicKeyLength, signatureLength);
        PublicKey senderPublicKey = crypto.getPublicKeyFromBytes(publicKeyBytes);
    
        // Authenticate
        if (!crypto.verifySignature(payload, signature, senderPublicKey)) {
            throw new Exception("[Receiver Node " + nodeId + "] Invalid signature!");
        }
    
        // Process message
        return processMessage(payload, packet);
    }

    private DatagramPacket receivePacketWithLogging() throws Exception {
        System.out.println("[Receiver Node " + nodeId + "] Waiting for packet...");
        DatagramPacket packet = networkClass.receivePacket();
        System.out.println("[Receiver Node " + nodeId + "] Received packet from " 
            + packet.getAddress().getHostAddress() + ":" + packet.getPort());
        return packet;
    }
    
    private byte[] extractPayload(byte[] data, int messageLength) {
        byte[] payload = new byte[messageLength];
        System.arraycopy(data, 0, payload, 0, messageLength);
        return payload;
    }
    
    private byte[] extractPublicKey(byte[] data, int offset, int length) {
        byte[] pubKey = new byte[length];
        System.arraycopy(data, offset, pubKey, 0, length);
        return pubKey;
    }
    
    private byte[] extractSignature(byte[] data, int offset, int pubKeyLen, int sigLen) {
        byte[] signature = new byte[sigLen];
        System.arraycopy(data, offset + pubKeyLen, signature, 0, sigLen);
        return signature;
    }
    
    private String processMessage(byte[] payload, DatagramPacket packet) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int sequenceNumber = buffer.getInt();
        byte[] message = new byte[payload.length - 4];
        buffer.get(message);
    
        System.out.println("[Receiver Node " + nodeId + "] Signature verified.");
        System.out.println("[Receiver Node " + nodeId + "] Checking for duplicate message...");
    
        if (!receivedSequenceNumbers.contains(sequenceNumber)) {
            receivedSequenceNumbers.add(sequenceNumber);
            sendACK(packet.getAddress(), packet.getPort());
            return new String(message);
        } else {
            throw new Exception("[Receiver Node " + nodeId + "] Duplicate message detected!");
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
