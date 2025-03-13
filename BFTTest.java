import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;


public class BFTTest {
    public static void main(String[] args) throws Exception {
        // Define ports.
        int leaderPort = 8000;          // Leader listens on this port.
        int serverClientPort1 = 7001;   // Server 0 listens for client messages on this port.
        int serverClientPort2 = 7002;   // Server 1 listens for client messages on this port.
        int serverClientPort3 = 7003;   // Server 2 listens for client messages on this port.

        // Generate key pairs.
        KeyPair leaderKP = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair serverKP1 = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair serverKP2 = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair serverKP3 = KeyGeneratorUtil.generateRSAKeyPair();
        KeyPair clientKP = KeyGeneratorUtil.generateRSAKeyPair();

        // Create crypto instances.
        cryptoClass leaderCrypto = new cryptoClass(leaderKP.getPrivate(), leaderKP.getPublic());
        cryptoClass serverCrypto1 = new cryptoClass(serverKP1.getPrivate(), serverKP1.getPublic());
        cryptoClass serverCrypto2 = new cryptoClass(serverKP2.getPrivate(), serverKP2.getPublic());
        cryptoClass serverCrypto3 = new cryptoClass(serverKP3.getPrivate(), serverKP3.getPublic());
        cryptoClass clientCrypto = new cryptoClass(clientKP.getPrivate(), clientKP.getPublic());

        // Create network instances.
        networkClass leaderNetwork = new networkClass(leaderPort);
        networkClass serverClientNetwork1 = new networkClass(serverClientPort1);
        networkClass serverClientNetwork2 = new networkClass(serverClientPort2);
        networkClass serverClientNetwork3 = new networkClass(serverClientPort3);
        networkClass clientNetwork = new networkClass(0); // Client uses an ephemeral port.

        // Set up server addresses.
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        List<InetAddress> serverAddresses = new ArrayList<>();
        serverAddresses.add(localhost); // All servers are on localhost.
        List<Integer> serverLeaderPorts = new ArrayList<>();
        serverLeaderPorts.add(leaderPort); // All servers send to leader on leaderPort.
        List<cryptoClass> serverCryptos = new ArrayList<>();
        serverCryptos.add(serverCrypto1);
        serverCryptos.add(serverCrypto2);
        serverCryptos.add(serverCrypto3);

        // Create leader and server instances.
        leaderBFT leader = new leaderBFT("Leader1", leaderNetwork, serverCryptos, serverAddresses, serverLeaderPorts);
        ServerBFT server0 = new ServerBFT("server0", serverClientNetwork1, leaderNetwork,
                serverCrypto1, clientCrypto, localhost, leaderPort);
        ServerBFT server1 = new ServerBFT("server1", serverClientNetwork2, leaderNetwork,
                serverCrypto2, clientCrypto, localhost, leaderPort);
        ServerBFT server2 = new ServerBFT("server2", serverClientNetwork3, leaderNetwork,
                serverCrypto3, clientCrypto, localhost, leaderPort);

        // Start leader and server threads.
        Thread leaderThread = new Thread(leader);
        Thread serverThread0 = new Thread(server0);
        Thread serverThread1 = new Thread(server1);
        Thread serverThread2 = new Thread(server2);
        leaderThread.start();
        serverThread0.start();
        serverThread1.start();
        serverThread2.start();

        // Let leader and servers start up.
        Thread.sleep(1000);

        // Client sends a message to all servers.
        Client client = new Client(clientNetwork, clientCrypto);
        String clientMessage = "TestMessage";
        System.out.println("[Client] Sending message: " + clientMessage);
        client.send(clientMessage, localhost, serverClientPort1); // Send to server0
        client.send(clientMessage, localhost, serverClientPort2); // Send to server1
        client.send(clientMessage, localhost, serverClientPort3); // Send to server2

        // Allow time for the protocol to execute.
        Thread.sleep(5000);

        // Print final blockchain states.
        System.out.println("\n[Final Results]");
        System.out.println("Leader blockchain: " + leader.getBlockchain());
        System.out.println("Server0 blockchain: " + server0.getBlockchain());
        System.out.println("Server1 blockchain: " + server1.getBlockchain());
        System.out.println("Server2 blockchain: " + server2.getBlockchain());

        // Stop the leader and server threads.
        leaderThread.interrupt();
        serverThread0.interrupt();
        serverThread1.interrupt();
        serverThread2.interrupt();

        // Clean up resources.
        leaderNetwork.closeSocket();
        serverClientNetwork1.closeSocket();
        serverClientNetwork2.closeSocket();
        serverClientNetwork3.closeSocket();
        clientNetwork.closeSocket();
    }
}