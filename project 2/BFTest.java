import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BFTest {
    private static List<Process> processes = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int N = 4; // Total number of nodes
        int f = 1; // Number of faulty nodes
        int LEADERPORT = 5000; // Base port for nodes
        Contract contract = new Contract();
        String clientAddress = contract.getClientAddress(); 
        String receiverAddress = contract.getReceiverAddress();
        String amount = "100";
        String data = contract.getData(receiverAddress, amount);

        // Shutdown hook for clean process termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down all processes...");
            processes.forEach(Process::destroy);
        }));

        // Start nodes
        startNode(1, LEADERPORT, N, f, true); // Leader
        for (int i = 2; i <= N; i++) {
            startNode(i, LEADERPORT + i - 1, N, f, false); // Followers
        }

        // Start clients after nodes are up
        Thread.sleep(2000);
        startClient(1, N, clientAddress, receiverAddress, amount, data);

        System.out.println("Started " + N + " nodes and 1 clients. System running...");
        Thread.sleep(30000); // Extended runtime for transaction processing
    }

    private static void startNode(int nodeId, int port, int N, int f, boolean isLeader) {
        try {
            Process process = new ProcessBuilder("java", "NodeBFT",
                    String.valueOf(nodeId),
                    String.valueOf(isLeader),
                    String.valueOf(port),
                    String.valueOf(N),
                    String.valueOf(f))
                    .inheritIO()
                    .start();
            
            processes.add(process);
            System.out.printf("Node %d started (Leader: %b) on port %d%n", 
                            nodeId, isLeader, port);
        } catch (IOException e) {
            System.err.printf("Failed to start Node %d: %s%n", nodeId, e.getMessage());
        }
    }

    private static void startClient(int clientId, int N, String sender, 
                                 String receiver, String amount, String data) {
        try {
            Process process = new ProcessBuilder("java", "ClientBFT",
                    String.valueOf(clientId),
                    String.valueOf(N),
                    sender,
                    receiver,
                    amount,
                    data)
                    .inheritIO()
                    .start();
            
            processes.add(process);
            System.out.printf("Client %d sending %s from %s to %s%n", 
                           clientId, amount, sender, receiver);
        } catch (IOException e) {
            System.err.printf("Failed to start Client %d: %s%n", clientId, e.getMessage());
        }
    }
}
