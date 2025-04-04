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
    public static List<Block> blockchain = new ArrayList<>();
    private final int CLIENTPORT = 6000;
    private final Contract contract = new Contract();

    public NodeBFT(int nodeId, boolean isLeader, int port, int N, int f) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
        this.port = port;
        this.N = N;
        this.f = f;
    }

    public void start() throws Exception {
        networkClass network = new networkClass(port);
        cryptoClass crypto = new cryptoClass();
        AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(network, crypto, nodeId);

        if (blockchain.isEmpty()) {
            Block block1 = new Block("0xc2abd98d5d011c420ba467bbb06a7d0ef591c03596a1dc5807a8c28b4b373b1a");
            blockchain.add(block1);
        }

        Predicate<Map<Integer, String>> predicateC = messages -> {
            int validStateCount = 0;
            for (String msg : messages.values()) {
                if (msg != null && msg.startsWith("STATE")) {
                    validStateCount++;
                }
            }
            return validStateCount >= N - f;
        };

        conditionalCollect cc = new conditionalCollect(apl, isLeader, N, f, predicateC);

        int ets = 1 + nodeId; 
        ByzantineEpochConsensus bec = new ByzantineEpochConsensus(nodeId, N, f, ets, isLeader, apl, cc);

        // Initialize the node's state
        if(!isLeader){
            String message = apl.receiveMessage();
            String[] parts = message.split(",");
            System.out.println("[Node " + nodeId + "] Received Transaction: " + message);
            Transaction transaction = new Transaction(parts[1], parts[2], parts[3], parts[4]);

            System.out.println("[Node " + nodeId + "] Transaction Hash: " + transaction.toString());
            blockchain.get(0).addTransaction(transaction);


            Map<String, Object> epochState = new HashMap<>();
            epochState.put("valts", ets);
            epochState.put("val", "Tx1");
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
            Transaction transaction = new Transaction(parts[1], parts[2], parts[3], parts[4]);
            System.out.println("[Node " + nodeId + "] Transaction Hash: " + transaction.toString());

            blockchain.get(0).addTransaction(transaction);
            bec.propose("Tx1");

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
                        List<Transaction> transactions = blockchain.get(0).getTransactions();
                        for (Transaction transaction : transactions) {
                            System.out.println("[Node " + nodeId + "] Handling transaction: " + transaction.toString());
                            contract.transfer(transaction.getSender(), transaction.getReceiver(), transaction.getAmount()); //executar transação nodes non-leader
                        }
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


        System.out.println("[Node " + nodeId + "] Consensus reached.");

        if(isLeader){
            System.out.println("[Node " + nodeId + "] Leader is here!!!!!!!!");
            String message = apl.receiveMessage();
            System.out.println("[Node " + nodeId + "] Received message: " + message + "from client");
            String[] parts = message.split(",");
            Block block = blockchain.get(0);
            List<Transaction> transactions = block.getTransactions();

            for (Transaction transaction : transactions) {
                System.out.println("[Node " + nodeId + "] Handling transaction: " + transaction.toString());
                contract.transfer(transaction.getSender(), transaction.getReceiver(), transaction.getAmount()); //executar transação node leader
            }

            for (Transaction transaction : transactions) {              //Avisa se a transação está na blockchain
                if(parts[0].equals("QUERY")&&transaction.get(0).getHash().equals(parts[1])){
                    apl.sendMessage("TRUE",InetAddress.getLocalHost(),CLIENTPORT);          
                } else {
                    apl.sendMessage("FALSE",InetAddress.getLocalHost(),CLIENTPORT);
                }
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