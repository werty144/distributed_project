package cs451.hosting;

import cs451.Message;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

public class Server {
    private Host host;
    private DatagramSocket UDPSocket;
    private Receiver receiver;
    private Sender sender;
    private List<Host> hosts;

    public Server(Host host, List<Host> hosts) {
        this.host = host;
        this.hosts = hosts;
        try {
            UDPSocket = new DatagramSocket(host.getPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        receiver = new Receiver(UDPSocket, this);
        sender = new Sender(UDPSocket);
    }

    public void start() {
        receiver.start();
        sender.start();
    }

    public void sendMessagePL(Message message) {
        sender.sendMessagePL(message);
    }

    public void receiveMessagePL(String ip, int port, String content) {
        System.out.println("Received message from " + ip + ":" + port + ". Content: " + content);
    }
}
