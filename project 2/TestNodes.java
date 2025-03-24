import java.util.ArrayList;
import java.util.List;

public class TestNodes {
    private static final int N = 4; // Total number of nodes
    private static final int f = 1; // Maximum number of faulty nodes

    public static void main(String[] args) {
        List<Process> processes = new ArrayList<>();

        // Start N nodes
        for (int i = 1; i <= N; i++) {
            boolean isLeader = (i == 1); // Node 1 is the leader
            int port = isLeader ? 5000 : 5000 + (i-1);  // Assign unique ports to each node

            ProcessBuilder pb = new ProcessBuilder("java", "Node", String.valueOf(i), String.valueOf(isLeader), String.valueOf(port), String.valueOf(N), String.valueOf(f));
            pb.inheritIO(); // Redirect process output to the main process

            try {
                Process process = pb.start();
                processes.add(process);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Wait for all processes to finish
        for (Process process : processes) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("All nodes have finished.");
    }
}
