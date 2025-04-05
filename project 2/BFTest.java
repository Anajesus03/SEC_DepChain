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
        String clientAddress = contract.getReceiverAddress(); 
        String receiverAddress = contract.getClientAddress(); 
        String amount = "0x100";
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
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(".:/home/rodrigo/Documents/Faculdade/SEC/lab/lab2/jars/*");  // Add your jars
        command.add("NodeBFT");
        command.add(String.valueOf(nodeId));
        command.add(String.valueOf(isLeader));
        command.add(String.valueOf(port));
        command.add(String.valueOf(N));
        command.add(String.valueOf(f));
    
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // redirect output
    
        try {
            Process process = pb.start();
            System.out.println("Started Node " + nodeId + " (Leader: " + isLeader + ") on port " + port);
            processes.add(process);
        } catch (IOException e) {
            e.printStackTrace();
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
