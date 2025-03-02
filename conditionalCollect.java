import java.util.*;
import java.util.concurrent.*;

public class conditionalCollect {

    // key is condition and value is set of messages
    private Map<String, Set<String>> collectedMessages = new ConcurrentHashMap<>();
    private int expectedMessages; // number of messages to satisfy the condition

    public conditionalCollect(int expectedMessages) {
        this.expectedMessages = expectedMessages;
    }

    // Collect a message that satisfies the condition
    public void collectMessage(String condition, String message){
        collectedMessages.computeIfAbsent(condition, k-> ConcurrentHashMap.newKeySet()).add(message);
        System.out.println("[Collector] Message collected for condition: " + condition);

    }

    // Check if the condition has been satisfied
    public boolean isConditionSatisfied(String condition){
        Set<String> messages = collectedMessages.get(condition);
        return messages != null && messages.size() >= expectedMessages;
    }

    // get all messages that satisfy the condition
    public Set<String> getMessages(String condition){
        return collectedMessages.getOrDefault(condition, Collections.emptySet());
    }
}
