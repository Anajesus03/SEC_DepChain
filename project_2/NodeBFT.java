import java.net.InetAddress;
import java.util.*;
import org.hyperledger.besu.datatypes.Address;
import java.util.function.Predicate;
import org.hyperledger.besu.evm.tracing.OperationTracer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NodeBFT {
    private final int nodeId;
    private final boolean isLeader;
    private final int port;
    private final int N;
    private final int f;
    public static List<Block> blockchain = new ArrayList<>();
    private final int CLIENTPORT = 6000;
    private final int CLIENTPORT2 = 6001;
    private Contract contract;
    private final int numberofClients = 2;

    private networkClass network;
    private cryptoClass crypto;
    private AuthenticatedPerfectLink apl;
    private conditionalCollect cc;
    private ByzantineEpochConsensus bec;

    public NodeBFT(int nodeId, boolean isLeader, int port, int N, int f) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
        this.port = port;
        this.N = N;
        this.f = f;
    }

    public void start() throws Exception {
        initializeNetworkAndContract();
        initializeGenesisBlock();
        prepareConsensusInfrastructure();

        if (!isLeader) {
            receiveClientTransactions();
            initializeEpochState(false);
        } else {
            initializeEpochState(true);
            leaderProposeAndCollect();
        }

        mainConsensusLoop();
        handlePostConsensus();

        Thread.sleep(10000);

        createJsons();
        bec.abort();
    }

    private void initializeNetworkAndContract() throws Exception {
        network = new networkClass(port);
        crypto = new cryptoClass();
        apl = new AuthenticatedPerfectLink(network, crypto, nodeId);
        contract = new Contract();
    }

    private void initializeGenesisBlock() {
        if (blockchain.isEmpty()) {
            Block block1 = new Block("0xc2abd98d5d011c420ba467bbb06a7d0ef591c03596a1dc5807a8c28b4b373b1a");
            blockchain.add(block1);
        }
    }

    private void prepareConsensusInfrastructure() {
        Predicate<Map<Integer, String>> predicateC = messages -> {
            int validStateCount = 0;
            for (String msg : messages.values()) {
                if (msg != null && msg.startsWith("STATE")) {
                    validStateCount++;
                }
            }
            return validStateCount >= N - f;
        };

        cc = new conditionalCollect(apl, isLeader, N, f, predicateC);
        int ets = 1 + nodeId;
        bec = new ByzantineEpochConsensus(nodeId, N, f, ets, isLeader, apl, cc);
    }

    private void receiveClientTransactions() throws Exception {
        int messagesreceived = 0;
        while (messagesreceived != numberofClients) {
            System.out.println("[Node " + nodeId + "] Waiting for transaction messages...");
            String message = apl.receiveMessage();
            String[] parts = message.split(",");
            System.out.println("[Node " + nodeId + "] Received Transaction: " + message);

            Transaction transaction;
            if (parts.length < 6) {
                transaction = new Transaction(parts[1], parts[2], parts[3], "");
                choiceFinder(parts[4]);
            } else {
                transaction = new Transaction(parts[1], parts[2], parts[3], parts[5]);
                choiceFinder(parts[4]);
            }
            transaction.signTransaction(crypto.getPrivateKey());

            blockchain.get(0).addTransaction(transaction);
            messagesreceived++;
        }
    }

    private void initializeEpochState(boolean isLeader) {
        Map<String, Object> epochState = new HashMap<>();
        int ets = 1 + nodeId;
        epochState.put("valts", ets);

        if (isLeader) {
            epochState.put("val", null);
        } else {
            List<Transaction> transactions = blockchain.get(0).getTransactions();
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < transactions.size(); i++) {
                valueBuilder.append(transactions.get(i).getHash());
                if (i != transactions.size() - 1) {
                    valueBuilder.append("-");
                }
            }
            epochState.put("val", valueBuilder.toString());
        }

        epochState.put("writeset", new HashSet<>());
        bec.init(epochState);
    }

    private void leaderProposeAndCollect() throws Exception {
        receiveClientTransactions();

        List<Transaction> transactions = blockchain.get(0).getTransactions();
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < transactions.size(); i++) {
            valueBuilder.append(transactions.get(i).getHash());
            if (i != transactions.size() - 1) {
                valueBuilder.append("-");
            }
        }

        Thread.sleep(10000);
        bec.propose(valueBuilder.toString());

        System.out.println("[Node " + nodeId + "] Collecting responses from non-leaders...");
        while (!cc.isCollected()) {
            cc.receiveMessage();
        }

        Map<Integer, String> collectedMessages = cc.getCollectMessages();
        collectedMessages.put(nodeId, bec.getLeaderState());
        bec.handleCollectedStates(collectedMessages);
        Thread.sleep(4000);
    }

    private void mainConsensusLoop() throws Exception {
        boolean terminated = false;
        while (!terminated) {
            if (bec.isDecided()) {
                System.out.println("Leader Terminated");
                break;
            }

            String message = apl.receiveMessage();
            if (message == null) continue;

            String type;
            String[] parts;
            if (message.equals("READ")) {
                type = "READ";
                parts = new String[0];
            } else {
                parts = message.split(",");
                type = parts[0];
            }

            switch (type) {
                case "READ" -> {
                    if (!isLeader) bec.handleReadMessage(nodeId);
                }
                case "WRITE" -> {
                    bec.handleWriteMessage(Integer.parseInt(parts[1]), parts[2]);
                    if (!isLeader) {
                        Thread.sleep(3000);
                        Map<Integer, String> collectedMessages = cc.getCollectMessages();
                        bec.handleCollectedStates(collectedMessages);
                    }
                }
                case "ACCEPT" -> {
                    bec.handleAcceptMessage(Integer.parseInt(parts[1]), parts[2]);
                    for (Transaction tx : blockchain.get(0).getTransactions()) {
                        if (tx.verifySignature(crypto.getPublicKey())) {
                            contract.transfer(tx.getSenderAddress(), tx.getReceiverAddress(), tx.getAmount());
                        }
                    }
                    terminated = true;
                }
                case "COLLECTED" -> cc.processMessage(message);
                default -> System.err.println("[Node " + nodeId + "] Unknown message type: " + type);
            }
        }
        System.out.println("[Node " + nodeId + "] Consensus reached.");
    }

    private void handlePostConsensus() throws Exception {
        if (!isLeader) return;

        for (Transaction tx : blockchain.get(0).getTransactions()) {
            if (tx.verifySignature(crypto.getPublicKey())) {
                contract.transfer(tx.getSenderAddress(), tx.getReceiverAddress(), tx.getAmount());
            }
        }

        int messagestosend = 0;
        while (messagestosend != numberofClients) {
            String message = apl.receiveMessage();
            String[] parts = message.split(",");
            int clientPort = parts[1].equals("5") ? CLIENTPORT : CLIENTPORT2;

            boolean found = false;
            for (Transaction tx : blockchain.get(0).getTransactions()) {
                if (parts[0].equals("QUERY") && tx.getHash().equals(parts[2])) {
                    apl.sendMessage("TRUE", InetAddress.getLocalHost(), clientPort);
                    found = true;
                    break;
                }
            }

            if (!found) {
                apl.sendMessage("FALSE", InetAddress.getLocalHost(), clientPort);
            }

            messagestosend++;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: Node <nodeId> <isLeader> <port> <N> <f>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        boolean isLeader = Boolean.parseBoolean(args[1]);
        int port = Integer.parseInt(args[2]);
        int N = Integer.parseInt(args[3]);
        int f = Integer.parseInt(args[4]);

        NodeBFT node = new NodeBFT(nodeId, isLeader, port, N, f);
        node.start();
    }

    public void createJsons() {
        Block block = blockchain.get(0);
        List<Transaction> transactions = block.getTransactions();
    
        // Make sure output directory exists
        File dir = new File("Transaction");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("❌ Failed to create Transaction/ directory.");
                return;
            }
        }
    
        Map<String, Map<String, String>> allTransactions = new LinkedHashMap<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
        for (int i = 0; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);
    
            Map<String, String> txData = new LinkedHashMap<>();
            txData.put("sender", tx.getSender());
            txData.put("senderBalance", String.valueOf(contract.getBalance(tx.getSenderAddress())));
            txData.put("receiver", tx.getReceiver());
            txData.put("receiverBalance", String.valueOf(contract.getBalance(tx.getReceiverAddress())));
            txData.put("amount", tx.getAmount());
            txData.put("data", tx.getData());
    
            allTransactions.put("Transaction " + i, txData);
        }
    
        String filename = "Transaction/all_transactions.json";
    
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(allTransactions, writer);
            System.out.println("✅ Created " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to write " + filename);
            e.printStackTrace();
        }
    }
    

    public void choiceFinder(String choice) {
        switch (choice) {
            case "1" -> addBlackListISTCoin();
            case "2" -> addBlackListOwner();
            case "3" -> addBlackListClient();
            default -> System.out.println("No blacklisting selected.");
        }
    }

    public void addBlackListISTCoin() {
        Address addr = Address.fromHexString("0x9876543210987654321098765432109876543210");
        contract.addToBlacklist(addr);
    }

    public void addBlackListOwner() {
        Address addr = Address.fromHexString("0x5B38Da6a701c568545dCfcB03FcB875f56beddC4");
        contract.addToBlacklist(addr);
    }

    public void addBlackListClient() {
        Address addr = Address.fromHexString("0xfeedfacefeedfacefeedfacefeedfacefeedface");
        contract.addToBlacklist(addr);
    }
}