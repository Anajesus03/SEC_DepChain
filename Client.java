import java.net.InetAddress;

public class Client {

    private AuthenticatedPerfectLink apl;

    public Client(networkClass network, cryptoClass crypto) {
        this.apl = new AuthenticatedPerfectLink(network, crypto);
    }

    public void send(String message, InetAddress dest, int port) throws Exception {
        apl.sendMessage(message, dest, port);
    }
    
}
