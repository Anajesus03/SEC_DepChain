import java.net.InetAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class ServerBFT extends Thread {
    private String name;
    private AuthenticatedPerfectLink apl;
    private cryptoClass leader;
    private List<String> blockchain;
    private int port;

    public ServerBFT(String name, networkClass network, cryptoClass leader, int port) {
        this.name = name;
        this.apl = new AuthenticatedPerfectLink(network, leader);
        this.leader = leader;
        this.blockchain = new ArrayList<>();
        this.port = port;
        System.out.println("[BFT Server] " + name + " initialized.");
    }

    @Override
    public void run() {
        System.out.println(name + " started on port " + this.port);
        try {
            Thread.sleep(3000); // Simulate some work
            System.out.println(name + " is working...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
