import java.net.InetAddress;

public class Sender {
    public static void main(String[] args) {
        try {
          
            
            // Create network and crypto class
            networkClass network = new networkClass(5000);
            cryptoClass crypto = new cryptoClass();
            int nodeId=2;
            AuthenticatedPerfectLink apLink = new AuthenticatedPerfectLink(network, crypto,nodeId);

            String message = "Hello, this is a test message!";
            InetAddress receiverAddress = InetAddress.getByName("localhost");
            int port = 6000;

            // Send message
            apLink.sendMessage(message, receiverAddress, port);
            String receivedMessage = apLink.receiveMessage();
            System.out.println("[Receiver] Received: " + receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
