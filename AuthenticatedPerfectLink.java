import java.net.*;
import java.security.*;

public class AuthenticatedPerfectLink {

    private networkClass networkClass;
    private cryptoClass crypto;

    public AuthenticatedPerfectLink(networkClass networkClass, cryptoClass crypto) {
        this.networkClass = networkClass;
        this.crypto = crypto;
    }

    // send message with authentication
    public void sendMessage(String message, InetAddress dest, int port) throws Exception {
        byte[] data = message.getBytes();
        byte[] signature = crypto.signMessage(data);

        byte[] messageWithSignature = new byte[data.length + signature.length];
        System.arraycopy(data, 0, messageWithSignature, 0, data.length);
        System.arraycopy(signature, 0, messageWithSignature, data.length, signature.length);

        networkClass.sendPacket(dest, port, messageWithSignature);

    }

    // receive message with authentication

    public String receiveMessage() throws Exception {
        DatagramPacket packet = networkClass.receivePacket();
        
        byte[] signature = new byte[crypto.getSignatureLength()];
        byte[] message = new byte[packet.getLength() - crypto.getSignatureLength()];

        System.arraycopy(packet.getData(), 0, message, 0, message.length);
        System.arraycopy(packet.getData(), message.length, signature, 0, signature.length);

        if (crypto.verifySignature(message, signature)) {
            return new String(message);
        } else {
            throw new Exception("Invalid signature");
        }
    }

    public void close() {
        networkClass.closeSocket();
    }
    
}
