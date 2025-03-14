import org.junit.jupiter.api.*;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticatedPerfectLinkTest {

    private networkClass senderNetwork;
    private networkClass receiverNetwork;
    private cryptoClass senderCrypto;
    private cryptoClass receiverCrypto;
    private AuthenticatedPerfectLink senderLink;
    private AuthenticatedPerfectLink receiverLink;
    private KeyPair senderKeyPair;
    private KeyPair receiverKeyPair;
    private final Object lock = new Object();  // Lock to synchronize thread actions.

    @BeforeEach
    public void setUp() throws Exception {
        // Generate a pair of RSA keys for sender and receiver
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        senderKeyPair = keyGen.generateKeyPair();
        receiverKeyPair = keyGen.generateKeyPair();

        // Create network and crypto instances for sender and receiver
        senderNetwork = new networkClass(5000); // Sender listens on port 5000
        receiverNetwork = new networkClass(6000); // Receiver listens on port 6000

        senderCrypto = new cryptoClass(senderKeyPair.getPrivate(), receiverKeyPair.getPublic());
        receiverCrypto = new cryptoClass(receiverKeyPair.getPrivate(), senderKeyPair.getPublic());

        // Create AuthenticatedPerfectLink instances for sender and receiver
        senderLink = new AuthenticatedPerfectLink(senderNetwork, senderCrypto);
        receiverLink = new AuthenticatedPerfectLink(receiverNetwork, receiverCrypto);
    }

    @AfterEach
    public void tearDown() {
        // Close the links after each test
        senderLink.close();
        receiverLink.close();
    }

    @Test
    public void testAuthenticatedPerfectLink() throws Exception {
        // Start a receiver thread to listen for incoming messages
        Thread receiverThread = new Thread(() -> {
            try {
                System.out.println("[Receiver] Waiting for message...");
                String receivedMessage = receiverLink.receiveMessage();
                System.out.println("[Receiver] Received message: " + receivedMessage);
                assertEquals("Hello, this is a secure message!", receivedMessage);
                
                // After first message is processed, receiver should acknowledge.
                synchronized (lock) {
                    lock.notify();
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail("Receiver threw an exception");
            }
        });
        receiverThread.start();

        // Give the receiver some time to start listening
        Thread.sleep(1000);

        // Send a message from the sender to the receiver
        InetAddress receiverAddress = InetAddress.getLocalHost();
        int receiverPort = 6000;
        String message = "Hello, this is a secure message!";
        System.out.println("[Sender] Sending message: " + message);

        senderLink.sendMessage(message, receiverAddress, receiverPort);

        // Wait for the receiver thread to finish
        receiverThread.join();

        // Now test for duplicate message detection
        System.out.println("\n=== Testing Duplicate Message Detection ===");

        // Reset the sequence number to 0 for the sender before sending the duplicate
        senderLink.sequenceNumbers = 0;

        // Start a new receiver thread for the duplicate test.
        Thread duplicateReceiverThread = new Thread(() -> {
            try {
                System.out.println("[Receiver-Duplicate] Waiting for duplicate message...");
                String duplicateMsg = receiverLink.receiveMessage();
                System.out.println("[Receiver-Duplicate] Received duplicate message: " + duplicateMsg);
                fail("Duplicate message detected, but no exception was thrown");
            } catch (Exception e) {
                // Expected: duplicate message detection should throw an exception.
                System.out.println("[Receiver-Duplicate] Exception: " + e.getMessage());
                assertTrue(e.getMessage().contains("Duplicate message"));
            }
        });
        duplicateReceiverThread.start();

        // Wait for the receiver to finish processing the first message
        synchronized (lock) {
            lock.wait();  // Wait until the receiver thread has processed the first message
        }

        // Send the message twice from the sender to the receiver
        System.out.println("[Sender] Sending message again: " + message);
        senderLink.sendMessage(message, receiverAddress, receiverPort);

        // Wait for the duplicate receiver thread to finish
        duplicateReceiverThread.join(5000); // Wait for the receiver thread to finish
    }
}