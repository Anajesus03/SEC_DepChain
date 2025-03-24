
import java.net.InetAddress;



public class ClientBFT {
    private AuthenticatedPerfectLink apl;
    private int N;
    private int LEADERPORT = 5000;
    private static int PORT = 6000;
    
        public ClientBFT(int clientId, AuthenticatedPerfectLink apl, int N) {
            this.apl = apl;
            this.N = N;
        }
    
        public void sendValueToNodes(String value) throws Exception{
            for(int nodeId=1; nodeId<=N; nodeId++){
                String message = "PROPOSE," + value;
                apl.sendMessage(message,InetAddress.getLocalHost(),LEADERPORT+(nodeId-1));
            }
        }
    
        public boolean isValueInBlockchain(String value) throws  Exception {
            String message = "QUERY," + value;
            apl.sendMessage(message,InetAddress.getLocalHost(),LEADERPORT);
    
            // Wait for response
            String response = apl.receiveMessage();
            return response != null && response.equals("TRUE");
        }
    
        public static void main(String[] args) throws Exception {
            if (args.length < 3) {
                System.out.println("Usage: ClientBFT <clientId> <N> <value>");
                return;
            }
            
            
    
            int clientId = Integer.parseInt(args[0]);
            int N = Integer.parseInt(args[1]);
            String value = args[2];
    
            networkClass network = new networkClass(PORT);
            cryptoClass crypto = new cryptoClass();
            AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(network, crypto, clientId);

            ClientBFT client = new ClientBFT(clientId, apl, N);

            client.sendValueToNodes(value);
            Thread.sleep(15000);

            //Query a node to check if the value is in the blockchain
           boolean isValueAppended = client.isValueInBlockchain(value);
            System.out.println("[Client " + clientId + "] Value '" + value + "' in blockchain: " + isValueAppended);
    }


}
