import java.net.InetAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class ServerBFT extends Thread {
    private String name;                      // Server name (e.g., "A", "B", "C", "D")
    private AuthenticatedPerfectLink apl;     // For secure communication
    private cryptoClass leader;              // Crypto instance for leader communication
    private List<String> blockchain;          // Simulated blockchain (message storage)
    private int port;                         // Server's port

    public ServerBFT(String name, networkClass network, cryptoClass leader, int port) {
        this.name = name;
        this.apl = new AuthenticatedPerfectLink(network, leader);
        this.leader = leader;
        this.blockchain = new ArrayList<>();
        this.port = port;
        System.out.println("[BFT Server] " + name + " initialized on port " + port);
    }

    @Override
    public void run() {
        System.out.println("[BFT Server] " + name + " started and listening on port " + this.port);

        try {
            while (true) {
                // Listen for incoming messages
                String message = apl.receiveMessage();
                System.out.println("[" + name + "] Received message: " + message);

                // Store message in the blockchain (log of messages)
                blockchain.add(message);
                System.out.println("[" + name + "] Updated Blockchain: " + blockchain);
            }
        } catch (Exception e) {
            System.err.println("[" + name + "] Error receiving message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            apl.close();  // Ensure socket is closed when thread ends
        }
    }
}
