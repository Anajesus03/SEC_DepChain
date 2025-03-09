import java.util.ArrayList;
import java.util.List;

public class ClientBFT extends Thread {
    private String name;
    private List<AuthenticatedPerfectLink> apl;
    private List<cryptoClass> servers;
    private List<String> blockchain;
    

    public ClientBFT(String name, networkClass network, List<cryptoClass> servers) {
        this.name = name;
        this.servers = servers;
        this.apl = new ArrayList<>();
        for (int i = 0; i < this.servers.size(); i++) {
            apl.add(new AuthenticatedPerfectLink(network, servers.get(i)));
        }
        this.blockchain = new ArrayList<>();
        System.out.println("[BFT Client] " + name + " initialized.");
    }

    @Override
    public void run() {
        System.out.println(name + " started on port " + 4000);
        try {
            Thread.sleep(3000); // Simulate some work
            System.out.println(name + " is working...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
/* 
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
*/

    public List<String> getBlockchain() {
        return blockchain;
    }

    public void close() {
        for (AuthenticatedPerfectLink link : apl) {
            link.close();
        }
    }
}
