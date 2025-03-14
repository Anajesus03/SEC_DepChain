import org.junit.jupiter.api.*;
import java.security.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConditionalCollectTest {

    private conditionalCollect collector;
    private Map<String, PublicKey> publicKeys;
    private KeyPair keyPair;

    @BeforeEach
    public void setUp() throws Exception {
        publicKeys = new HashMap<>();

        // Generate a key pair for testing
        keyPair = generateKeyPair();
        publicKeys.put("server1", keyPair.getPublic());

        collector = new conditionalCollect(2, publicKeys);
    }

    @Test
    public void testCollectValidMessage() throws Exception {
        String condition = "conditionA";
        String message = "Test message";

        byte[] signature = signMessage(message, keyPair.getPrivate());

        collector.collectMessage("server1", condition, message, signature);

        assertFalse(collector.isConditionSatisfied(condition));

        collector.collectMessage("server1", condition, "Another message", signMessage("Another message", keyPair.getPrivate()));

        assertTrue(collector.isConditionSatisfied(condition));
    }

    @Test
    public void testCollectInvalidSignature() {
        String condition = "conditionB";
        String message = "Invalid message";

        byte[] fakeSignature = new byte[256];
        new Random().nextBytes(fakeSignature);

        Exception exception = assertThrows(Exception.class, () -> {
            collector.collectMessage("server1", condition, message, fakeSignature);
        });

        assertTrue(exception.getMessage().contains("Invalid signature"));
    }

    @Test
    public void testUnknownServer() {
        String condition = "conditionC";

        Exception exception = assertThrows(Exception.class, () -> {
            collector.collectMessage("unknownServer", condition, "Message", new byte[256]);
        });

        assertTrue(exception.getMessage().contains("Public key not found"));
    }

    @Test
    public void testClearCondition() throws Exception {
        String condition = "conditionD";
        String message = "Clear this";

        collector.collectMessage("server1", condition, message, signMessage(message, keyPair.getPrivate()));

        assertFalse(collector.getMessages(condition).isEmpty());

        collector.clearCondition(condition);

        assertTrue(collector.getMessages(condition).isEmpty());
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private byte[] signMessage(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        return signature.sign();
    }
}