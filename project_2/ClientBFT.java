import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Base64;

public class ClientBFT {
    private AuthenticatedPerfectLink apl;
    private int N;
    private int LEADERPORT = 5000;
    private static int PORT = 6000;
    private String clientAddress;
    private cryptoClass crypto;
    private String choice;
    private int clientId;
    
    public ClientBFT(int clientId, AuthenticatedPerfectLink apl, int N, String clientAddress, String choice) throws Exception {
        this.clientAddress = clientAddress;
        this.apl = apl;
        this.N = N;
        this.crypto = new cryptoClass();
        this.clientId = clientId;
        this.choice = choice;
    }

    public void sendTransaction(String receiver, String amount, String data) throws Exception {
        // Create transaction string: sender,receiver,amount,data
        String transaction = clientAddress + "," + receiver + "," + amount + "," + choice + "," + data; 
        System.out.println("[Client] Sending transaction: " + transaction);
        for(int nodeId=1; nodeId<=N; nodeId++){
                String message = "PROPOSE," + transaction;
                apl.sendMessage(message, InetAddress.getLocalHost(), LEADERPORT+(nodeId-1));
        }
    }

    public boolean verifyTransaction(String txHash) throws Exception {
        String message = "QUERY,"+ clientId + "," + txHash;
        apl.sendMessage(message, InetAddress.getLocalHost(), LEADERPORT);

        // Wait for response
        String response = apl.receiveMessage();
        return response != null && response.equals("TRUE");
    }

    public static String calculateHash(String sender, String receiver, String amount, String data) {
        try {
            String input = sender + receiver + amount + data;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error computing hash", e);
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Usage: ClientBFT <clientId> <N> <clientAddress> <receiver> <amount> [data] [choice]");
            return;
        }
        
        int clientId = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);
        String clientAddress = args[2];
        String receiver = args[3];
        String amount = args[4];
        String choice = args[5];
        String data = args.length > 6 ? args[6] : "";
        int Clientport = (clientId==5)?PORT:PORT+1; // clientId 1 -> port 6000, clientId 2 -> port 6001: 

        networkClass network = new networkClass(Clientport); 
        cryptoClass crypto = new cryptoClass();
        AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(network, crypto, clientId);

        ClientBFT client = new ClientBFT(clientId, apl, N, clientAddress, choice);

        // Send transaction to nodes
        client.sendTransaction(receiver, amount, data);
        Thread.sleep(25000);

        // Create transaction hash for verification (would normally come from node response)
        String txHash = calculateHash(clientAddress, receiver, amount, data);
        
        // Verify if transaction was included in blockchain
        boolean isTxConfirmed = client.verifyTransaction(txHash);
        System.out.println("[Client " + clientId + "] Transaction present in the Blockchain: " + isTxConfirmed);
    }

}