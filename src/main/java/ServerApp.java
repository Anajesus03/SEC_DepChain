import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class ServerApp {
/*    public static void main(String[] args) {
        try {
            // Create 4 Normal Servers on different ports
            List<String> name_list = Arrays.asList("A", "B", "C", "D");

            // Generate RSA KeyPairs for client, servers, and leader
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);

            KeyPair keyPairClient = keyGen.generateKeyPair();
            KeyPair keyPairServerA = keyGen.generateKeyPair();
            KeyPair keyPairServerB = keyGen.generateKeyPair();
            KeyPair keyPairServerC = keyGen.generateKeyPair();
            KeyPair keyPairServerD = keyGen.generateKeyPair();
            KeyPair keyPairLeader = keyGen.generateKeyPair();

            // Create crypto instances for communication
            cryptoClass cryptoClient_ServerA = new cryptoClass(keyPairClient.getPrivate(), keyPairServerA.getPublic());
            cryptoClass cryptoClient_ServerB = new cryptoClass(keyPairClient.getPrivate(), keyPairServerB.getPublic());
            cryptoClass cryptoClient_ServerC = new cryptoClass(keyPairClient.getPrivate(), keyPairServerC.getPublic());
            cryptoClass cryptoClient_ServerD = new cryptoClass(keyPairClient.getPrivate(), keyPairServerD.getPublic());
            cryptoClass cryptoClient_Leader = new cryptoClass(keyPairClient.getPrivate(), keyPairLeader.getPublic());

            cryptoClass cryptoServerA_Client = new cryptoClass(keyPairServerA.getPrivate(), keyPairClient.getPublic());
            cryptoClass cryptoServerB_Client = new cryptoClass(keyPairServerB.getPrivate(), keyPairClient.getPublic());
            cryptoClass cryptoServerC_Client = new cryptoClass(keyPairServerC.getPrivate(), keyPairClient.getPublic());
            cryptoClass cryptoServerD_Client = new cryptoClass(keyPairServerD.getPrivate(), keyPairClient.getPublic());

            // Start the Client
            new Thread(new ClientBFT("Client", new networkClass(4000), Arrays.asList(cryptoClient_ServerA, cryptoClient_ServerB, cryptoClient_ServerC, cryptoClient_ServerD, cryptoClient_Leader))).start();

            // Start the 4 Normal Servers on different ports
            new Thread(new ServerBFT("A", new networkClass(5000), cryptoServerA_Client, 5000)).start();
            new Thread(new ServerBFT("B", new networkClass(5001), cryptoServerB_Client, 5001)).start();
            new Thread(new ServerBFT("C", new networkClass(5002), cryptoServerC_Client, 5002)).start();
            new Thread(new ServerBFT("D", new networkClass(5003), cryptoServerD_Client, 5003)).start();

            System.out.println("All servers are running...");
        } catch (NoSuchAlgorithmException | SocketException e) {
            e.printStackTrace();
        }
    }
        */
}