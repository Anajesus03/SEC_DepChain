import java.net.DatagramPacket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class authenticatedPerfectLinkTestClass {

    public static void main(String[] args) {
        try {
            // Generate a key pair for testing
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Create cryptoClass instances for sender and receiver
            cryptoClass senderCrypto = new cryptoClass(keyPair.getPrivate(), keyPair.getPublic());
            cryptoClass receiverCrypto = new cryptoClass(keyPair.getPrivate(), keyPair.getPublic());

            // Create networkClass instances for sender and receiver
            networkClass senderNetwork = new networkClass(5000);
            networkClass receiverNetwork = new networkClass(5001);

            // Create AuthenticatedPerfectLink instances for sender and receiver
            AuthenticatedPerfectLink senderLink = new AuthenticatedPerfectLink(senderNetwork, senderCrypto);
            AuthenticatedPerfectLink receiverLink = new AuthenticatedPerfectLink(receiverNetwork, receiverCrypto);

            // Test 1: Send and receive an authenticated message
            System.out.println("Test 1: Send and receive an authenticated message");

            // Test message
            String message = "Hello, DepChain!";

            // Send the message from sender to receiver
            System.out.println("Sending message: " + message);
            senderLink.sendMessage(message, InetAddress.getByName("127.0.0.1"), 5001);

            // Receive the message on the receiver side
            System.out.println("Waiting to receive message...");
            String receivedMessage = receiverLink.receiveMessage();
            System.out.println("Received message: " + receivedMessage);

            // Assert that the received message matches the sent message
            if (message.equals(receivedMessage)) {
                System.out.println("Test 1 passed: Received message matches sent message.");
            } else {
                System.out.println("Test 1 failed: Received message does not match sent message.");
            }

            // Test 2: Test handling of tampered messages
            System.out.println("\nTest 2: Test handling of tampered messages");

            // Send a valid message
            String validMessage = "Valid message";
            System.out.println("Sending valid message: " + validMessage);
            senderLink.sendMessage(validMessage, InetAddress.getByName("127.0.0.1"), 5001);

            // Receive the packet (before tampering)
            System.out.println("Receiving the packet...");
            DatagramPacket receivedPacket = receiverNetwork.receivePacket();

            // Tamper with the received packet (simulate an attack)
            System.out.println("Tampering with the received packet...");
            byte[] tamperedData = receivedPacket.getData();
            tamperedData[0] = (byte) ~tamperedData[0]; // Flip a bit in the message

            // Create a new tampered packet
            DatagramPacket tamperedPacket = new DatagramPacket(
                tamperedData, 
                tamperedData.length, 
                receivedPacket.getAddress(), 
                receivedPacket.getPort()
            );

            // Attempt to process the tampered packet
            System.out.println("Attempting to process the tampered packet...");
            try {
                // Manually extract the message and signature from the tampered packet
                byte[] signature = new byte[receiverCrypto.getSignatureLength()];
                byte[] messageBytes = new byte[tamperedPacket.getLength() - receiverCrypto.getSignatureLength()];

                System.arraycopy(tamperedPacket.getData(), 0, messageBytes, 0, messageBytes.length);
                System.arraycopy(tamperedPacket.getData(), messageBytes.length, signature, 0, signature.length);

                // Verify the tampered message
                if (receiverCrypto.verifySignature(messageBytes, signature)) {
                    System.out.println("Test 2 failed: Tampered message was not rejected.");
                } else {
                    System.out.println("Test 2 passed: Tampered message was rejected.");
                }
            } catch (Exception e) {
                System.out.println("Test 2 passed: Tampered message was rejected (exception thrown).");
            }

            // Close links
            senderLink.close();
            receiverLink.close();

        } catch (Exception e) {
            System.err.println("An error occurred during testing: " + e.getMessage());
        }
    }
}
