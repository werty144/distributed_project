package cs451.hosting;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;
import java.util.Set;


public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<String> PLMessages = new HashSet<>();
    private Server server;
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
    }

    public void run() {
        while (true) {
            receiveFLL();
        }
    }

    private void receiveFLL() {
        try {
            UDPSocket.receive(packet);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        receiveSL(packet);
    }

    private void receiveSL(DatagramPacket packet) {
        receivePL(packet);
    }

    private void receivePL(DatagramPacket packet) {
        String content = new String(packet.getData(), 0, packet.getLength());
        String ip = packet.getAddress().toString();
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        String uniqueID = ip + '$' + packet.getPort() + '$' + content;
        if (!PLMessages.contains(uniqueID)) {
            PLMessages.add(uniqueID);
            server.receiveMessagePL(ip, packet.getPort(), content);
        }
    }
}

