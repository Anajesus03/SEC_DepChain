import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class AuthenticatedPerfectLink {

    private networkClass networkClass;
    private cryptoClass crypto;
    private int sequenceNumbers=0; // AP2: No duplication
    private Set<Integer> receivedSequenceNumbers = new HashSet<>(); 
    private static final int MAX_UDP_SIZE = 65507; // Maximum size of a UDP packet

    public AuthenticatedPerfectLink(networkClass networkClass, cryptoClass crypto) {
        this.networkClass = networkClass;
        this.crypto = crypto;
    }

    // send message with authentication
    public void sendMessage(String message, InetAddress dest, int port) throws Exception {
        System.out.println("[Sender] Preparing message: " + message);
        byte[] data = message.getBytes();
        byte[] sequenceNumberBytes = ByteBuffer.allocate(4).putInt(sequenceNumbers).array();
        byte[] dataWithSequenceNumber = new byte[data.length + sequenceNumberBytes.length];
        System.arraycopy(sequenceNumberBytes, 0, dataWithSequenceNumber, 0, sequenceNumberBytes.length);
        System.arraycopy(data, 0, dataWithSequenceNumber, sequenceNumberBytes.length, data.length);

        System.out.println("[Sender] Signing message...");
        byte[] signature = crypto.signMessage(dataWithSequenceNumber);
        byte[] messageWithSignature = new byte[dataWithSequenceNumber.length + signature.length];
        System.arraycopy(dataWithSequenceNumber, 0, messageWithSignature, 0, dataWithSequenceNumber.length);
        System.arraycopy(signature, 0, messageWithSignature, dataWithSequenceNumber.length, signature.length);

        boolean receivedACK = false; // AP1: Reliable delivery

        // checks if the message is too large to be sent
        if (messageWithSignature.length > MAX_UDP_SIZE) {
            throw new Exception("Message too large");
        }

        while(!receivedACK) {
            System.out.println("[Sender] Sending message to " + dest.getHostAddress() + ":" + port);
            networkClass.sendPacket(dest, port, messageWithSignature);
            try {
                System.out.println("[Sender] Waiting for ACK...");
                DatagramPacket ackPacket = receiveACKWithTimeout(1000);
                if(ackPacket != null) {
                    receivedACK = true;
                    System.out.println("[Sender] Received ACK from " + ackPacket.getAddress().getHostAddress() + ":" + ackPacket.getPort());
                }
            } catch (Exception e) {
                System.out.println("Timeout, resending message");
            }
        }

        sequenceNumbers++;
        System.out.println("[Sender] Message sent successfully.");
    }

    // receive message with authentication

    public String receiveMessage() throws Exception {
        System.out.println("[Receiver] Waiting for packet...");
        DatagramPacket packet = networkClass.receivePacket();
        System.out.println("[Receiver] Received packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

        byte[] signature = new byte[crypto.getSignatureLength()];
        byte[] message = new byte[packet.getLength() - crypto.getSignatureLength()];

        System.arraycopy(packet.getData(), 0, message, 0, message.length);
        System.arraycopy(packet.getData(), message.length, signature, 0, signature.length);

        System.out.println("[Receiver] Verifying signature...");
        if (crypto.verifySignature(message, signature)) {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            int sequenceNumber = buffer.getInt();
            byte[] payload = new byte[message.length-4];
            buffer.get(payload);

            // AP2: No duplication
            System.out.println("[Receiver] Checking for duplicate message...");
            if(!receivedSequenceNumbers.contains(sequenceNumber)) {
                receivedSequenceNumbers.add(sequenceNumber);
                System.out.println("[Receiver] Sending ACK...");
                sendACK(packet.getAddress(), packet.getPort()); // send ACK to sender
                System.out.println("[Receiver] Processing message: " + new String(payload));
                return new String(payload);
            } else {
                throw new Exception("Duplicate message");
            }
        } else {
            throw new Exception("Invalid signature");
        }
    }

    // function to send ACK messages
    private void sendACK(InetAddress address, int port) throws Exception {
        System.out.println("[Receiver] Sending ACK to " + address.getHostAddress() + ":" + port);
        byte[] ack = "ACK".getBytes();
        networkClass.sendPacket(address, port, ack);
    }

    // function to receive ACK messages with timeout
    private DatagramPacket receiveACKWithTimeout(int timeout) throws Exception {
        System.out.println("[Sender] Waiting for ACK with timeout " + timeout + "ms...");
        byte[] aux = new byte[4];
        DatagramPacket ackPacket = new DatagramPacket(aux, aux.length);
        networkClass.getSocket().setSoTimeout(timeout);

        try {
            networkClass.getSocket().receive(ackPacket);
            if (new String(ackPacket.getData(),0, ackPacket.getLength()).equals("ACK")) {
                System.out.println("[Sender] ACK received.");
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
    
}
