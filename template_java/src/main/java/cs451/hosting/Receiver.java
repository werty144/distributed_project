package cs451.hosting;

import cs451.Message;

import java.awt.image.DataBufferUShort;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;
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
        if (!PLMessages.contains(content)) {
            PLMessages.add(content);
            server.receiveMessagePL(packet.getAddress().toString(), packet.getPort(), content);
        }
    }
}
