import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BFTest {
    private static List<Process> processes = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int N = 4; // Total number of nodes
        int f = 1; // Number of faulty nodes
        int LEADERPORT = 5000; // Port for the leader

        // Add a shutdown hook to clean up processes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down all nodes...");
            for (Process process : processes) {
                process.destroy();
            }
        }));

        // Start the leader node (nodeId = 1)
        startNode(1, LEADERPORT, N, f, true);

        // Start non-leader nodes (nodeId = 2, 3, ...)
        for (int i = 1; i <= (N - 1); i++) {
            int nodePort = LEADERPORT + i; // Assign unique ports to non-leaders
            int nodeId = i + 1;
            startNode(nodeId, nodePort, N, f, false);
        }

        ProcessBuilder clientPb = new ProcessBuilder("java", "ClientBFT", "1", String.valueOf(N), "Test Message 123...");
        clientPb.inheritIO();
        processes.add(clientPb.start());

        System.out.println("All nodes started. Waiting for consensus...");
        
        // Keep the main thread alive while nodes run
        Thread.sleep(20000); 
    }

    private static void startNode(int nodeId, int port, int N, int f, boolean isLeader) {
        ProcessBuilder pb = new ProcessBuilder("java", "NodeBFT",
                String.valueOf(nodeId),
                String.valueOf(isLeader),
                String.valueOf(port),
                String.valueOf(N),
                String.valueOf(f));

        pb.inheritIO(); // Redirect output to the parent process

        try {
            Process process = pb.start();
            processes.add(process);
            System.out.println("Started Node " + nodeId + " (Leader: " + isLeader + ") on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

