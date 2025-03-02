import java.net.InetAddress;
import java.util.ArrayList;
import java.security.PublicKey;
import java.util.List;

public class ClientBFT {
    private String name;
    private AuthenticatedPerfectLink apl;
    private InetAddress leaderAddress;
    private int leaderPort;
    private BlockChain blockchain;
    private PublicKey leaderPublicKey;

    public ClientBFT(String name, networkClass network, cryptoClass crypto, InetAddress leaderAddress, int leaderPort, PublicKey leaderPublicKey) {
        this.name = name;
        this.apl = new AuthenticatedPerfectLink(network, crypto);
        this.leaderAddress = leaderAddress;
        this.leaderPort = leaderPort;
        this.blockchain = new BlockChain(leaderPublicKey);
        System.out.println("[BFT Client] " + name + " initialized.");
    }

    // Listen for messages from the leader
    public void startListening() {
        System.out.println("[Client " + name + "] Listening for messages...");
        while (true) {
            try {
                String message = apl.receiveMessage();

                if (message.startsWith("DECIDED:")) {
                    handleDecision(message.substring(8).trim()); // Process finalized decision
                } else {
                    handleProposal(message.trim()); // Process new proposal
                }

            } catch (Exception e) {
                System.err.println("[Client " + name + "] Error receiving message: " + e.getMessage());
            }
        }
    }

    // Handle proposals from the leader
    private void handleProposal(String proposal) {
        System.out.println("[Client " + name + "] Received proposal: " + proposal);

        // Simple validation logic (replace with your actual validation rules)
        boolean isValid = validateProposal(proposal);

        // Send vote back to the leader
        try {
            String response = isValid ? "ACCEPT" : "REJECT";
            apl.sendMessage(response, leaderAddress, leaderPort);
            System.out.println("[Client " + name + "] Sent vote: " + response);
        } catch (Exception e) {
            System.err.println("[Client " + name + "] Error sending vote: " + e.getMessage());
        }
    }

    // Validate proposal 
    private boolean validateProposal(String proposal) {
        // Example check: reject empty proposals
        return proposal != null && !proposal.isEmpty();
    }

    // Process the leader's final decision
    private void handleDecision(String value) {
        System.out.println("[Client " + name + "] Decision received: " + value);
        
        if (!blockchain.contains(value)) {
            blockchain.add(value);
            System.out.println("[Client " + name + "] Updated blockchain: " + blockchain);
        } else {
            System.out.println("[Client " + name + "] Duplicate decision ignored.");
        }
    }

    public BlockChain getBlockchain() {
        return blockchain;
    }

    public void close() {
        apl.close();
    }
}
