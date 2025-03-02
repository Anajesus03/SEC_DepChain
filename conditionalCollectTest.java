import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class conditionalCollectTest {
    public static void main(String[] args) throws Exception {
        // Generate key pairs for cryptographic operations
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();

        // Initialize network and crypto classes
        networkClass network1 = new networkClass(5000);
        networkClass network2 = new networkClass(5001);
        cryptoClass crypto1 = new cryptoClass(keyPair1.getPrivate(), keyPair2.getPublic());
        cryptoClass crypto2 = new cryptoClass(keyPair2.getPrivate(), keyPair1.getPublic());

        // Initialize AuthenticatedPerfectLink instances
        AuthenticatedPerfectLink apl1 = new AuthenticatedPerfectLink(network1, crypto1);
        AuthenticatedPerfectLink apl2 = new AuthenticatedPerfectLink(network2, crypto2);

        // Initialize ConditionalCollect instances with a quorum size of 2
        conditionalCollect cc1 = new conditionalCollect(1);
        conditionalCollect cc2 = new conditionalCollect(2);

        // Define a condition
        String condition = "CONDITION_1";

        // Simulate message exchange and conditional collect
        System.out.println("Testing Conditional Collect...");

        // Node 1 collects a message
        cc1.collectMessage(condition, "MESSAGE_1");

        // Node 2 collects a message
        cc2.collectMessage(condition, "MESSAGE_2");

        // Verify that the condition is satisfied
        System.out.println("Condition satisfied for cc1: " + cc1.isConditionSatisfied(condition));
        System.out.println("Condition satisfied for cc2: " + cc2.isConditionSatisfied(condition));

        // Verify collected messages
        System.out.println("Collected messages for cc1: " + cc1.getMessages(condition));
        System.out.println("Collected messages for cc2: " + cc2.getMessages(condition));

        // Close resources
        apl1.close();
        apl2.close();
    }
}
