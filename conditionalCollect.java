import java.util.*;
import java.util.concurrent.*;

public class conditionalCollect {

    // key is condition and value is set of messages
    private Map<String, Set<Map.Entry<String,byte[]>>> collectedMessages = new ConcurrentHashMap<>();
    private int expectedMessages; // number of messages to satisfy the condition
    private cryptoClass crypto;

    public conditionalCollect(int expectedMessages,cryptoClass crypto) {
        this.crypto = crypto;
        this.expectedMessages = expectedMessages;
    }

    // Collect a message that satisfies the condition
    public void collectMessage(String condition, String message, byte[] signature) throws Exception {
        
        if(crypto.verifySignature(message.getBytes(), signature)){
            collectedMessages.computeIfAbsent(condition, k-> ConcurrentHashMap.newKeySet()).add(new AbstractMap.SimpleEntry<>(message,signature));
            System.out.println("[Collector] Message collected for condition: " + condition);

        } else{
            throw new Exception("Invalid signature for message: " + message);
        }
        

    }

    // Check if the condition has been satisfied
    public boolean isConditionSatisfied(String condition){
        Set<Map.Entry<String, byte[]>> messages = collectedMessages.get(condition);
        return messages != null && messages.size() >= expectedMessages;
    }

    // get all messages that satisfy the condition
    public Set<Map.Entry<String, byte[]>> getMessages(String condition){
        return collectedMessages.getOrDefault(condition, Collections.emptySet());
    }
}
