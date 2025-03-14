import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BFTTest {

    private Thread leaderThread;
    private Thread serverThread;
    private leaderBFT leader;
    private ServerBFT server;
    private Client client;
    private networkClass leaderNetwork;
    private networkClass serverClientNetwork;
    private networkClass clientNetwork;

    @BeforeEach
    public void setUp() throws Exception {
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
        leaderNetwork = new networkClass(leaderPort);
        serverClientNetwork = new networkClass(serverClientPort);
        clientNetwork = new networkClass(0); // Client uses an ephemeral port.

        // Set up server addresses.
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        List<InetAddress> serverAddresses = new ArrayList<>();
        serverAddresses.add(localhost);
        List<Integer> serverLeaderPorts = new ArrayList<>();
        serverLeaderPorts.add(leaderPort); // Server sends to leader on leaderPort.
        List<cryptoClass> serverCryptos = new ArrayList<>();
        serverCryptos.add(serverCrypto);

        // Create leader and server instances.
        leader = new leaderBFT("Leader1", leaderNetwork, serverCryptos, serverAddresses, serverLeaderPorts);
        server = new ServerBFT("server0", serverClientNetwork, leaderNetwork,
                serverCrypto, clientCrypto, localhost, leaderPort);

        // Create client instance.
        client = new Client(clientNetwork, clientCrypto);
    }

    @Test
    public void testBFTProtocol() throws Exception {
        // Start leader and server threads.
        leaderThread = new Thread(leader);
        serverThread = new Thread(server);
        leaderThread.start();
        serverThread.start();

        // Let leader and server start up.
        Thread.sleep(1000);

        // Client sends a message to the server.
        String clientMessage = "TestMessage";
        System.out.println("[Client] Sending message: " + clientMessage);
        client.send(clientMessage, InetAddress.getLocalHost(), 7001);

        // Allow time for the protocol to execute.
        Thread.sleep(1000);

        // Print final blockchain states.
        System.out.println("\n[Final Results]");
        System.out.println("Leader blockchain: " + leader.getBlockchain());
        System.out.println("Server0 blockchain: " + server.getBlockchain());

        // Assert that the blockchain contains the test message (basic validation)
        assertTrue(leader.getBlockchain().contains(clientMessage), "Leader's blockchain does not contain the test message");
        assertTrue(server.getBlockchain().contains(clientMessage), "Server's blockchain does not contain the test message");

        // Stop the leader and server threads.
        leaderThread.interrupt();
        serverThread.interrupt();

        // Clean up resources.
        leaderNetwork.closeSocket();
        serverClientNetwork.closeSocket();
        clientNetwork.closeSocket();
    }
}
