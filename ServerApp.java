import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class ServerApp {
    public static void main(String[] args) {
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

            cryptoClass cryptoServerA_Leader = new cryptoClass(keyPairServerA.getPrivate(), keyPairLeader.getPublic());
            cryptoClass cryptoServerB_Leader = new cryptoClass(keyPairServerB.getPrivate(), keyPairLeader.getPublic());
            cryptoClass cryptoServerC_Leader = new cryptoClass(keyPairServerC.getPrivate(), keyPairLeader.getPublic());
            cryptoClass cryptoServerD_Leader = new cryptoClass(keyPairServerD.getPrivate(), keyPairLeader.getPublic());

            cryptoClass cryptoLeader_ServerA = new cryptoClass(keyPairLeader.getPrivate(), keyPairServerA.getPublic());
            cryptoClass cryptoLeader_ServerB = new cryptoClass(keyPairLeader.getPrivate(), keyPairServerB.getPublic());
            cryptoClass cryptoLeader_ServerC = new cryptoClass(keyPairLeader.getPrivate(), keyPairServerC.getPublic());
            cryptoClass cryptoLeader_ServerD = new cryptoClass(keyPairLeader.getPrivate(), keyPairServerD.getPublic());

            // Store crypto instances in lists
            List<cryptoClass> cryptoServerList = Arrays.asList(cryptoServerA_Leader, cryptoServerB_Leader, cryptoServerC_Leader, cryptoServerD_Leader);
            List<cryptoClass> cryptoClientList = Arrays.asList(cryptoClient_ServerA, cryptoClient_ServerB, cryptoClient_ServerC, cryptoClient_ServerD, cryptoClient_Leader);
            List<cryptoClass> cryptoLeaderList = Arrays.asList(cryptoLeader_ServerA, cryptoLeader_ServerB, cryptoLeader_ServerC, cryptoLeader_ServerD);

            // Start the Client
            new Thread(new ClientBFT("Client", new networkClass(4000), cryptoClientList)).start();

            // Start the 4 Normal Servers on different ports
            for (int i = 0; i < 4; i++) {
                new Thread(new ServerBFT(name_list.get(i), new networkClass(5000 + i), cryptoServerList.get(i), 5000 + i)).start();
            }

            // Create Leader Server
            new Thread(new leaderBFT("Leader", new networkClass(6000), cryptoLeaderList)).start();

            System.out.println("All servers are running...");
        } catch (NoSuchAlgorithmException | SocketException e) {
            e.printStackTrace();
        }
    }
}
