import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class BFTTest {
    public static void main(String[] args) throws Exception {
        // Define ports.
        int leaderPort = 8000;          // Leader listens on this port.
        int serverClientPort = 7001;    // Server listens for client messages on this port.

        // Generate key pairs.
        KeyPair leaderKP = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair serverKP = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair clientKP = KeyGeneratorUtil.generateRSAKeyPair();

        // Create crypto instances.
        cryptoClass leaderCrypto = new cryptoClass(leaderKP.getPrivate(), leaderKP.getPublic());
        cryptoClass serverCrypto = new cryptoClass(serverKP.getPrivate(), serverKP.getPublic());
        cryptoClass clientCrypto = new cryptoClass(clientKP.getPrivate(), clientKP.getPublic());

        // Create network instances.
        networkClass leaderNetwork = new networkClass(leaderPort);
        networkClass serverClientNetwork = new networkClass(serverClientPort);
        networkClass clientNetwork = new networkClass(0); // Client uses an ephemeral port.

        // Set up server addresses.
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        List<InetAddress> serverAddresses = new ArrayList<>();
        serverAddresses.add(localhost);
        List<Integer> serverLeaderPorts = new ArrayList<>();
        serverLeaderPorts.add(leaderPort); // Server sends to leader on leaderPort.
        List<cryptoClass> serverCryptos = new ArrayList<>();
        serverCryptos.add(serverCrypto);

        // Start leader thread.
        leaderBFT leader = new leaderBFT("Leader1", leaderNetwork, serverCryptos, serverAddresses, serverLeaderPorts);
        leader.start();

        // Start server thread.
        ServerBFT server = new ServerBFT("server0", serverClientNetwork, leaderNetwork,
                serverCrypto, clientCrypto, localhost, leaderPort);
        server.start();

        // Let server and leader start up.
        Thread.sleep(1000);

        // Client sends a message to the server.
        Client client = new Client(clientNetwork, clientCrypto);
        String clientMessage = "TestMessage";
        System.out.println("[Client] Sending message: " + clientMessage);
        client.send(clientMessage, localhost, serverClientPort);

        // Allow time for the protocol to execute.
        Thread.sleep(5000);

        // Print final blockchain states.
        System.out.println("\n[Final Results]");
        System.out.println("Leader blockchain: " + leader.getBlockchain());
        System.out.println("Server0 blockchain: " + server.getBlockchain());

        System.exit(0);
    }

}
