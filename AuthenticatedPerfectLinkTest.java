import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class AuthenticatedPerfectLinkTest {

    public static void main(String[] args) {
        try {
            // Generate a pair of RSA keys for sender and receiver
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair senderKeyPair = keyGen.generateKeyPair();
            KeyPair receiverKeyPair = keyGen.generateKeyPair();

            // Create network and crypto instances for sender and receiver
            networkClass senderNetwork = new networkClass(5000); // Sender listens on port 5000
            networkClass receiverNetwork = new networkClass(6000); // Receiver listens on port 6000

            cryptoClass senderCrypto = new cryptoClass(senderKeyPair.getPrivate(), receiverKeyPair.getPublic());
            cryptoClass receiverCrypto = new cryptoClass(receiverKeyPair.getPrivate(), senderKeyPair.getPublic());

            // Create AuthenticatedPerfectLink instances for sender and receiver
            AuthenticatedPerfectLink senderLink = new AuthenticatedPerfectLink(senderNetwork, senderCrypto);
            AuthenticatedPerfectLink receiverLink = new AuthenticatedPerfectLink(receiverNetwork, receiverCrypto);
            
            // Start a receiver thread to listen for incoming messages
            Thread receiverThread = new Thread(() -> {
                try {
                    System.out.println("[Receiver] Waiting for message...");
                    String receivedMessage = receiverLink.receiveMessage();
                    System.out.println("[Receiver] Received message: " + receivedMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiverThread.start();

            // Give the receiver some time to start listening
            Thread.sleep(1000);

            // Send a message from the sender to the receiver
            System.out.println("\n=== Normal Functioning ===");
            InetAddress receiverAddress = InetAddress.getLocalHost();
            int receiverPort = 6000;
            String message = "Hello, this is a secure message!";
            System.out.println("[Sender] Sending message: " + message);
            senderLink.sendMessage(message, receiverAddress, receiverPort);

            // Wait for the receiver thread to finish
            receiverThread.join();

            System.out.println("\n=== Testing Duplicate Message Detection and Reliable delivery ===");
            senderLink.sequenceNumbers = 0; // Reset the sequence number

            // Start a new receiver thread for the duplicate test.
            Thread duplicateReceiverThread = new Thread(() -> {
                try {
                    System.out.println("[Receiver-Duplicate] Waiting for duplicate message...");
                    String duplicateMsg = receiverLink.receiveMessage();
                    System.out.println("[Receiver-Duplicate] Received duplicate message: " + duplicateMsg);
                } catch (Exception e) {
                    // Expected: duplicate message detection should throw an exception.
                    System.out.println("[Receiver-Duplicate] Exception: " + e.getMessage());
                }
            });
            duplicateReceiverThread.start();

            Thread.sleep(1000);

            System.out.println("[Sender] Sending message: " + message);
            senderLink.sendMessage(message, receiverAddress, receiverPort);

            duplicateReceiverThread.join(5000); // Wait for the receiver thread to finish

            // Close the links
            senderLink.close();
            receiverLink.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}