import java.net.InetAddress;

public class Client {

    private AuthenticatedPerfectLink apl;

    public Client(networkClass network, cryptoClass crypto) {
        this.apl = new AuthenticatedPerfectLink(network, crypto);
    }

    public void send(String message, InetAddress dest, int port) throws Exception {
        apl.sendMessage(message, dest, port);
    }

    public boolean isValueadded(String value, InetAddress dest, int port) throws Exception {
        String message = "QUERY::" + value;
        apl.sendMessage(message, dest, port);
        String response = apl.receiveMessage();
        if (response.startsWith("RESPONSE::")) {
            String[] parts = response.split("::");
            if (parts.length == 2) {
                return Boolean.parseBoolean(parts[1]); // Parse the response
            }
        }
        throw new Exception("Invalid response from server or leader");
    }
    
}
