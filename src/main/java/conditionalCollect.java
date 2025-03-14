import java.security.PublicKey;
import java.security.Signature;
import java.util.*;
import java.util.concurrent.*;

public class conditionalCollect {

    // key is condition and value is set of messages
    private Map<String, Set<Map.Entry<String,byte[]>>> collectedMessages = new ConcurrentHashMap<>();
    private int expectedMessages; 
    private Map<String, PublicKey> publicKeys;

    public conditionalCollect(int expectedMessages, Map<String, PublicKey> publicKeys) {
        this.publicKeys = publicKeys;
        this.expectedMessages = expectedMessages;
    }

    // Collect a message that satisfies the condition
    public synchronized void collectMessage(String serverId,String condition, String message, byte[] signature) throws Exception {
        PublicKey publicKey = publicKeys.get(serverId);
        if(publicKey == null){
            throw new Exception("Public key not found for server: " + serverId);
        }
        if(verifySignature(publicKey,message.getBytes(), signature)){
            collectedMessages.computeIfAbsent(condition, k-> ConcurrentHashMap.newKeySet()).add(new AbstractMap.SimpleEntry<>(message,signature));
            System.out.println("[Collector] Message collected for condition: " + condition);

        } else{
            throw new Exception("Invalid signature for message: " + message);
        }
        

    }

    // Clears the collected messages for a condition.
    public synchronized void clearCondition(String condition) {
        collectedMessages.remove(condition);
    }

    // Check if the condition has been satisfied
    public boolean isConditionSatisfied(String condition){
        Set<Map.Entry<String, byte[]>> messages = collectedMessages.get(condition);
        return messages != null && messages.size() >= expectedMessages;
    }

    // get all messages that satisfy the condition
    public synchronized Set<Map.Entry<String, byte[]>> getMessages(String condition){
        return collectedMessages.getOrDefault(condition, Collections.emptySet());
    }

    public boolean verifySignature(PublicKey publicKey,byte[] message, byte[] signatureBytes) throws Exception {
        if (message == null || message.length == 0 || signatureBytes == null || signatureBytes.length == 0) {
            throw new IllegalArgumentException("Message and signature must not be null or empty");
        }
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(publicKey);
        sign.update(message);
        return sign.verify(signatureBytes);
    }
}
