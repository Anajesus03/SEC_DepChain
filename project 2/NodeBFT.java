import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class NodeBFT {
    private final int nodeId;
    private final boolean isLeader;
    private final int port;
    private final int N;
    private final int f;
    public static List<String> blockchain = new ArrayList<>();

    public NodeBFT(int nodeId, boolean isLeader, int port, int N, int f) {
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

        // Define the predicate C to check for valid states
        Predicate<Map<Integer, String>> predicateC = messages -> {
            int validStateCount = 0;
            for (String msg : messages.values()) {
                if (msg != null && msg.startsWith("STATE")) {
                    validStateCount++;
                }
            }
            return validStateCount >= N - f;
        };

        // Create the conditionalCollect instance
        conditionalCollect cc = new conditionalCollect(apl, isLeader, N, f, predicateC);

        int ets = 1 + nodeId; // Epoch timestamp (same for all nodes in the same epoch)
        // Initialize ByzantineEpochConsensus
        ByzantineEpochConsensus bec = new ByzantineEpochConsensus(nodeId, N, f, ets, isLeader, apl, cc);

        // Initialize the node's state
        if(!isLeader){
            String message = apl.receiveMessage();
            String[] parts = message.split(",");
            String value = parts[1];
            Map<String, Object> epochState = new HashMap<>();
            epochState.put("valts", ets);
            epochState.put("val", value);
            epochState.put("writeset", new HashSet<>());
            bec.init(epochState);
        } else {
            Map<String, Object> epochState = new HashMap<>();
            epochState.put("valts", ets);
            epochState.put("val", null);
            epochState.put("writeset", new HashSet<>());
            bec.init(epochState);
        }
        

        // Leader proposes a value
        if (isLeader) {
            String message = apl.receiveMessage();
            String[] parts = message.split(",");
            System.out.println("[Node " + nodeId + "] Proposing value: " + parts[1]);
            bec.propose(parts[1]);

            // Leader collects responses from non-leaders
            System.out.println("[Node " + nodeId + "] Collecting responses from non-leaders...");
            while (!cc.isCollected()) {
                cc.receiveMessage();
            }

            System.out.println("[Node " + nodeId + "] Processing collected states...");
            Map<Integer, String> collectedMessages = cc.getCollectMessages();
            String leaderState = bec.getLeaderState();
            collectedMessages.put(nodeId, leaderState);
            System.out.println("[Node leader " + nodeId + "] Collected messages: " + collectedMessages);
            bec.handleCollectedStates(collectedMessages);
            Thread.sleep(4000);
        }

        // Main message handling loop
        boolean terminated = false;
        while (!terminated) {
            if(bec.isDecided()){
                System.out.println("Leader Terminated");
                break;
            }
            String message = apl.receiveMessage();
            if (message != null) {
                System.out.println("[Node " + nodeId + "] Received message: " + message);
                String[] parts = message.split(",");
                String type = parts[0];

                switch (type) {
                    case "READ" -> {
                        if (!isLeader) {
                            System.out.println("[Node " + nodeId + "] Handling READ message...");
                            bec.handleReadMessage(nodeId);
                        }
                    }
                    case "WRITE" -> {
                        System.out.println("[Node " + nodeId + "] Handling WRITE message...");
                        bec.handleWriteMessage(Integer.parseInt(parts[1]), parts[2]);
                        if (!isLeader) {
                            Thread.sleep(3000);
                            Map<Integer, String> collectedMessages = cc.getCollectMessages();
                            bec.handleCollectedStates(collectedMessages);
                        }
                        
                    }
                    case "ACCEPT" -> {
                        System.out.println("[Node " + nodeId + "] Handling ACCEPT message...");
                        bec.handleAcceptMessage(Integer.parseInt(parts[1]), parts[2]);
                        blockchain.add(parts[2]);
                        terminated = true;
                    }
                    case "COLLECTED" -> {
                        System.out.println("[Node " + nodeId + "] Handling COLLECTED message...");
                        cc.processMessage(message);
                    }
                    default -> System.err.println("[Node " + nodeId + "] Unknown message type: " + type);
                }
            }

        }


        System.out.println("[Node " + nodeId + "] Consensus reached. Blockchain: " + blockchain);

        if(isLeader){
            System.out.println("[Node " + nodeId + "] Leader is here!!!!!!!!");
            String message = apl.receiveMessage();
            System.out.println("[Node " + nodeId + "] Received message: " + message + "from client");
            String[] parts = message.split(",");
            if(parts[0].equals("QUERY")&&blockchain.contains(parts[1])){
                apl.sendMessage("TRUE",InetAddress.getLocalHost(),6000);
            } else {
                apl.sendMessage("FALSE",InetAddress.getLocalHost(),6000);
            }
        }

        Thread.sleep(10000);
        // Abort the epoch
        bec.abort();
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
            NodeBFT node = new NodeBFT(nodeId, isLeader, port, N, f);
            node.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
