import java.io.*; 
import java.net.*;

public class networkClass {
    //Handles message transmission and reception using UDP

    private final DatagramSocket socket;
    private final int BUFFER_SIZE = 4096;

    public networkClass(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public void sendPacket(InetAddress address,int port, byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public void closeSocket() {
        socket.close();
    }
   
}