import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class AuthenticatedPerfectLinkTest {

    public static void main(String[] args) throws Exception {
        // Generate RSA key pair for crypto
        System.out.println("Generating RSA key pair...");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        System.out.println("RSA key pair generated.");

        // Ports for sender and receiver
        int senderPort = 5000;
        int receiverPort = 6000;

        // Create network and crypto instances for sender and receiver
        System.out.println("Creating network and crypto instances...");
        networkClass senderNetwork = new networkClass(senderPort);
        networkClass receiverNetwork = new networkClass(receiverPort);

        cryptoClass senderCrypto = new cryptoClass(keyPair.getPrivate(), keyPair.getPublic());
        cryptoClass receiverCrypto = new cryptoClass(keyPair.getPrivate(), keyPair.getPublic());
        System.out.println("Network and crypto instances created.");

        // Create AuthenticatedPerfectLink instances
        System.out.println("Creating AuthenticatedPerfectLink instances...");
        AuthenticatedPerfectLink sender = new AuthenticatedPerfectLink(senderNetwork, senderCrypto);
        AuthenticatedPerfectLink receiver = new AuthenticatedPerfectLink(receiverNetwork, receiverCrypto);
        System.out.println("AuthenticatedPerfectLink instances created.");

        // Start receiver in a separate thread
        System.out.println("Starting receiver thread...");
        Thread receiverThread = new Thread(() -> {
            try {
                System.out.println("[Receiver] Waiting for message...");
                String receivedMessage = receiver.receiveMessage();
                System.out.println("[Receiver] Received message: " + receivedMessage);
            } catch (Exception e) {
                System.out.println("[Receiver] Error: " + e.getMessage());
            } finally {
                System.out.println("[Receiver] Closing...");
                receiver.close();
            }
        });
        receiverThread.start();

        // Simulate sender sending a message
        Thread.sleep(1000); // Wait for receiver to start
        System.out.println("[Sender] Sending message...");
        sender.sendMessage("Hello, Authenticated Perfect Link!", InetAddress.getByName("localhost"), receiverPort);
        System.out.println("[Sender] Message sent.");

        // Wait for receiver to finish
        System.out.println("Waiting for receiver thread to finish...");
        receiverThread.join();

        // Close sender
        System.out.println("[Sender] Closing...");
        sender.close();
        System.out.println("Test completed.");
    }
}