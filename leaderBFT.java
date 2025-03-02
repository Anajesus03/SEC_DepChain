import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class leaderBFT {
    private AuthenticatedPerfectLink apl;
    private List<InetAddress> clients;
    private List<Integer> clientsPorts;
    private List<String> blockchain;

    public leaderBFT(networkClass network, cryptoClass crypto, List<InetAddress> clients, List<Integer> clientsPorts) {
        this.apl = new AuthenticatedPerfectLink(network, crypto);
        this.clients = clients;
        this.clientsPorts = clientsPorts;
        this.blockchain = new ArrayList<>();
    }

    public void propose(String value) throws Exception{
        System.out.println("[Leader] Proposing value: " + value);

        // send value to all clients
        for(int i=0;i<clients.size();i++) {
            InetAddress dest = clients.get(i);
            int port = clientsPorts.get(i);
            apl.sendMessage(value, dest, port);
        }

        // collect responses from the clients with values: ACCEPT or REJECT
        int acceptsReceived = 0;
        int rejectsReceived = 0;

        for(int i=0; i<clients.size();i++){
            String response = apl.receiveMessage();
            if(response.equals("ACCEPT")){
                acceptsReceived++;
            } else if(response.equals("REJECT")){
                rejectsReceived++;
            }
        }

        // Decide based on the majority of responses
        if(acceptsReceived > rejectsReceived){
            System.out.println("[Leader] Majority of ACCEPTS received. Adding value to blockchain.");
            blockchain.add(value);
            broadcastDecision(value);
        } else {
            System.out.println("[Leader] Majority of REJECTS received or draw. Value not added to blockchain.");
        }
    }

    private void broadcastDecision(String value) throws Exception{
        System.out.println("[Leader] Broadcasting decision to all clients: " + value);
        for(int i=0;i<clients.size();i++) {
            InetAddress dest = clients.get(i);
            int port = clientsPorts.get(i);
            apl.sendMessage("DECIDED: " + value, dest, port);
        }

    }

    public List<String> getBlockchain() {
        return blockchain;
    }
    
}
