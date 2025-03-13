import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ServerBFT implements Runnable {
    private String name;                      // Server name (e.g., "A", "B", "C", "D")
    private AuthenticatedPerfectLink clientAPL; // For receiving client messages.
    private AuthenticatedPerfectLink leaderAPL; // For sending to leader.
    private cryptoClass serverCrypto;
    private cryptoClass clientCrypto; // Used to verify client messages.
    private List<String> blockchain;          // Simulated blockchain (message storage)
    private InetAddress leaderAddress;
    private int leaderPort;                         

    public ServerBFT(String name, networkClass clientNetwork,networkClass leaderNetwork,cryptoClass serverCrypto,cryptoClass clientCrypto, InetAddress leaderAddress, int leaderPort) {
        this.leaderAddress = leaderAddress;
        this.leaderPort = leaderPort;
        this.name = name;
        // Use clientAPL for receiving messages from the client (verify using client's key).
        this.clientAPL = new AuthenticatedPerfectLink(clientNetwork, clientCrypto);
        // Use leaderAPL for sending to leader (sign with server's own key).
        this.leaderAPL = new AuthenticatedPerfectLink(leaderNetwork, serverCrypto);
        this.serverCrypto = serverCrypto;
        this.clientCrypto = clientCrypto;
        this.blockchain = new ArrayList<>();
        System.out.println("[BFT Server] " + name + " initialized.");
    }

    @Override
    public void run() {
        System.out.println("[" + name + "] started.");
        try {
            // First, receive the client message (sent using APL).
            String clientMessage = clientAPL.receiveMessage();
            System.out.println("[" + name + "] Received client message: " + clientMessage);
            
            // Compute the conditional (CC) signature over the payload.
            byte[] ccSignature = serverCrypto.signMessage(clientMessage.getBytes());
            String ccSignatureB64 = Base64.getEncoder().encodeToString(ccSignature);
            
            // Create a new message for the leader: payload::Base64(CC_signature)
            String messageForLeader = clientMessage + "::" + ccSignatureB64;
            System.out.println("[" + name + "] Forwarding message to leader: " + messageForLeader);
            
            // Send the message to the leader.
            leaderAPL.sendMessage(messageForLeader, leaderAddress, leaderPort);
            
            // Wait for the leader's decision.
            System.out.println("[" + name + "] Waiting for leader's decision...");
            String decision = leaderAPL.receiveMessage();
            if(decision.startsWith("DECIDE::")) {
                String value = decision.split("::")[1];
                blockchain.add(value);
                System.out.println("[" + name + "] Received decision from leader: " + value);
            } else {
                System.err.println("[" + name + "] Error: Unexpected message from leader: " + decision);
            }
        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> getBlockchain() {
        return blockchain;
    }
}
