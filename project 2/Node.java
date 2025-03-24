import java.util.Map;
import java.util.function.Predicate;

public class Node {
    private final int nodeId;
    private final boolean isLeader;
    private final int port;
    private final int N;
    private final int f;

    public Node(int nodeId, boolean isLeader, int port, int N, int f) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
        this.port = port;
        this.N = N;
        this.f = f;
    }

    public void start() throws Exception {
        // Initialize network, crypto, and authenticated perfect link
        networkClass network = new networkClass(port);
        cryptoClass crypto = new cryptoClass();
        AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(network, crypto, nodeId);
        boolean terminated = false;

        // Define the predicate C (example: at least N - f messages contain "valid")
        Predicate<Map<Integer, String>> predicateC = messages -> {
            int validCount = 0;
            for (String msg : messages.values()) {
                if (msg.contains("valid")) {
                    validCount++;
                }
            }
            return validCount >= N - f;
        };

        // Create the conditionalCollect instance
        conditionalCollect cc = new conditionalCollect(apl, isLeader, N, f, predicateC);

        // Simulate sending a message (non-leader nodes)
        if (!isLeader) {
            // Add a dynamic delay based on nodeId
            int delay = nodeId * 1000; // 1 second delay per nodeId
            System.out.println("[Node " + nodeId + "] Waiting for " + delay + "ms before sending the message...");
            Thread.sleep(delay);

            String message = "valid message from Node " + nodeId;
            cc.inputMessage(nodeId, message);

            // Add a delay to give the leader time to process the message
            System.out.println("[Node " + nodeId + "] Waiting for leader to process the message...");
            Thread.sleep(5000); // 5-second delay
        }

        // Leader listens for messages in a loop
        if (isLeader) {
            System.out.println("[Node " + nodeId + "] I am the leader. Listening for messages...");
            while (!terminated) {
                // Check if the collection is complete before receiving more messages
                if (cc.isCollected()) {
                    System.out.println("[Node " + nodeId +"Leader" + "] Messages collected. Terminating...");
                    terminated = true;
                } else {
                    cc.receiveMessage();
                }
            }
        } else {
            // Non-leader nodes also listen for the COLLECTED message
            System.out.println("[Node " + nodeId + "] I am a non-leader. Listening for COLLECTED message...");
            while (!terminated) {
                // Check if the collection is complete before receiving more messages
                if (cc.isCollected()) {
                    System.out.println("[Node " + nodeId + "] Messages collected. Terminating...");
                    terminated = true;
                } else {
                    cc.receiveMessage();
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: Node <nodeId> <isLeader> <port> <N> <f>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        boolean isLeader = Boolean.parseBoolean(args[1]);
        int port = Integer.parseInt(args[2]);
        int N = Integer.parseInt(args[3]);
        int f = Integer.parseInt(args[4]);

        try {
            Node node = new Node(nodeId, isLeader, port, N, f);
            node.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}