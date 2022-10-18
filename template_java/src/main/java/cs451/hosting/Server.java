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
        sender = new Sender(UDPSocket, this);
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

    public void stop() {
        receiver.interrupt();
        sender.interrupt();
    }

    public void sendMessagePL(Message message) {
        Logs.add("b " + message.getContent());
        sender.sendMessagePL(message);
    }

    public void receiveMessageFLL(String ip, int port, String message) {
        acknowledge(ip, port, message);
    }

    void acknowledge(String ip, int port, String message) {
        Host sender = getHost(ip, port);
        Message ack = new Message("ack$" + message, this.host, sender);
        this.sender.sendMessageFLL(ack);
    }

    public void receiveAcknowledgement(String ip, int port, String message) {
        Host sender = getHost(ip, port);
        this.sender.acknowledged(sender, message);
    }

    public void receiveMessagePL(String ip, int port, String content) {
        int senderID = getHost(ip,  port).getId();
        Logs.add("d " + senderID + " " + content);
    }

    Host getHost(String ip, int port) {
        Optional<Host> host = hosts.stream().filter(h -> (h.getIp().equals(ip)) && (h.getPort() == port)).findAny();
        assert host.isPresent();
        return host.get();
    }
}
