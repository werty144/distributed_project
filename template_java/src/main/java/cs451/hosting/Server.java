package cs451.hosting;

import cs451.Message;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class Server {
    private Host host;
    private DatagramSocket UDPSocket;
    private Receiver receiver;
    private Sender sender;
    private List<Host> hosts;
    private final List<String> Logs = Collections.synchronizedList(new ArrayList<>());

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

    public Host getHost() {
        return host;
    }

    public List<String> getLogs() {
        return Logs;
    }

    public void start() {
        receiver.start();
        sender.start();
    }

    public void sendMessagePL(Message message) {
        Logs.add("b " + message.getId());
        sender.sendMessagePL(message);
    }

    public void receiveMessagePL(String ip, int port, String content) {
        int senderID = getHostID(ip,  port);
        Logs.add("d " + senderID + " " + content);
    }

    int getHostID(String ip, int port) {
        Optional<Host> host = hosts.stream().filter(h -> (h.getIp().equals(ip)) && (h.getPort() == port)).findAny();
        assert host.isPresent();
        return host.get().getId();
    }
}
