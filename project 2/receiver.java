import java.net.InetAddress;

public class receiver {
    public static void main(String[] args) {
        try {
           
            // Create network and crypto class
            networkClass network = new networkClass(6000);
            cryptoClass crypto = new cryptoClass();
            int nodeId=1;
            AuthenticatedPerfectLink apLink = new AuthenticatedPerfectLink(network, crypto,nodeId);

            // Receive message
            String receivedMessage = apLink.receiveMessage();
            System.out.println("[Receiver] Received: " + receivedMessage);
            String message = "Hello, this is a test message again!";
            InetAddress receiverAddress = InetAddress.getByName("localhost");
            int port = 5000;
            apLink.sendMessage(message, receiverAddress, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
