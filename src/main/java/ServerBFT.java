import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ServerBFT implements Runnable {
    private String name;                      
    private AuthenticatedPerfectLink clientAPL; 
    private AuthenticatedPerfectLink leaderAPL; 
    private cryptoClass serverCrypto;
    private cryptoClass clientCrypto; 
    private List<String> blockchain;         
    private InetAddress leaderAddress;
    private int leaderPort;                         

    public ServerBFT(String name, networkClass clientNetwork,networkClass leaderNetwork,cryptoClass serverCrypto,cryptoClass clientCrypto, InetAddress leaderAddress, int leaderPort) {
        this.leaderAddress = leaderAddress;
        this.leaderPort = leaderPort;
        this.name = name;
        this.clientAPL = new AuthenticatedPerfectLink(clientNetwork, clientCrypto);
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
            // Receive the client message (sent using APL).
            String clientMessage = clientAPL.receiveMessage();
            System.out.println("[" + name + "] Received client message: " + clientMessage);
            if(clientMessage.startsWith("QUERY::")){
                String value = clientMessage.split("::")[1];
                boolean isValueadded =  blockchain.contains(value);
                String response = "RESPONSE::" + isValueadded;
                clientAPL.sendMessage(response, clientAPL.getSocket().getInetAddress(), clientAPL.getSocket().getPort()); 
                
            }else{
            
            byte[] ccSignature = serverCrypto.signMessage(clientMessage.getBytes());
            String ccSignatureB64 = Base64.getEncoder().encodeToString(ccSignature);
            
            String messageForLeader = clientMessage + "::" + ccSignatureB64;
            System.out.println("[" + name + "] Forwarding message to leader: " + messageForLeader);

            leaderAPL.sendMessage(messageForLeader, leaderAddress, leaderPort);
            
            System.out.println("[" + name + "] Waiting for leader's decision...");
            String decision = leaderAPL.receiveMessage();
            if(decision.startsWith("DECIDE::")) {
                String value = decision.split("::")[1];
                blockchain.add(value);
                System.out.println("[" + name + "] Received decision from leader: " + value);
            } else {
                System.err.println("[" + name + "] Error: Unexpected message from leader: " + decision);
            }

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
