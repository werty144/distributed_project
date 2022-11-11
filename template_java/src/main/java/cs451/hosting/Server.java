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
    public List<Host> hosts;
    public final List<String> Logs = Collections.synchronizedList(new ArrayList<>());
    public final List<FIFOMessage> deliveredFIFO = Collections.synchronizedList(new ArrayList<>());

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

    public void start() {
        receiver.start();
        sender.start();
    }

    public void stop() {
        receiver.interrupt();
        sender.interrupt();
    }

    public void sendMessagePL(Message message) {
        sender.sendMessagePL(message);
    }

    public void sendMessageFLL(Message message) {
        sender.sendMessageFLL(message);
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
    }

    Host getHost(String ip, int port) {
        Optional<Host> host = hosts.stream().filter(h -> (h.getIp().equals(ip)) && (h.getPort() == port)).findAny();
        assert host.isPresent();
        return host.get();
    }

    void suspect(Integer hostID) {
        System.out.println("Suspecting " + hostID + "\n");
    }

    public void bestEffortBroadcast(BEBMessage message) {
        sender.bestEffortBroadCast(message);
    }

    public void URBBroadcast(String content) {
        BEBMessage message = new BEBMessage(host.getId(), content);
        Logs.add("b " + message.content);
        bestEffortBroadcast(message);
    }

    public void URBDeliver(BEBMessage message) {
        deliveredFIFO.add(new FIFOMessage(message.SenderID, message.content, Integer.parseInt(message.content)));
    }
}
