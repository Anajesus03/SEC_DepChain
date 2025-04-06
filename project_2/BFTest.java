import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class BFTest {
    private static List<Process> processes = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int N = 4; // Total number of nodes
        int f = 1; // Number of faulty nodes
        int LEADERPORT = 5000; // Base port for nodes
    
        Contract contract = new Contract();
    
        String clientAddress = contract.getClientAddress(); 
        String receiverAddress = contract.getReceiverAddress();
        String ISTcoinAddress = contract.getISTCoinContractAddress(); 
        String amount = "0x100";
        String amount2 = "0x200";
    
        String data = contract.getData(ISTcoinAddress, amount);
        String data2 = contract.getData(clientAddress, amount2);
    
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nChoose an execution mode:");
        System.out.println("0 - Execute without Blacklisting ISTCoin");
        System.out.println("1 - Execute with ISTCoin Blacklisted");
        System.out.println("2 - Execute with Owner Blacklisted");
        System.out.println("3 - Execute with Client Blacklisted");
        System.out.print("Enter your choice (0/1/2/3): ");
    
        int Choice = Integer.parseInt(scanner.nextLine().trim());
    
        switch (Choice) {
            case 0:
                System.out.println("Executing without Blacklisting any Account...");
                break;
            case 1:
                System.out.println("Blacklisting ISTCoin...");
                break;
            case 2:
                System.out.println("Blacklisting Owner...");
                break;
            case 3:
                System.out.println("Blacklisting Client...");
                break;
            default:
                System.out.println("Invalid input. Exiting...");
                return;
        }

        String choice = String.valueOf(Choice); // Convert choice to String for client
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
        startClient(5, N, clientAddress, ISTcoinAddress, amount, data, choice);
        Thread.sleep(5000);
        startClient(6, N, receiverAddress, clientAddress, amount2, data2, choice);

        System.out.println("Started " + N + " nodes and 2 clients. System running...");
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
                                 String receiver, String amount, String data, String choice) {
        try {
            Process process = new ProcessBuilder("java", "ClientBFT",
                    String.valueOf(clientId),
                    String.valueOf(N),
                    sender,
                    receiver,
                    amount,
                    choice,
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