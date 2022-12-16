package cs451.hosting;

import cs451.Message;
import cs451.parsing.MessageParser;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

import static java.lang.Math.max;

public class Server {
    private Host host;
    private DatagramSocket UDPSocket;
    private Receiver receiver;
    private Sender sender;
    public List<Host> hosts;
    public LatticeAcceptor latticeAcceptor;
    public LatticeProposer latticeProposer;
    public final List<String> logs = Collections.synchronizedList(new ArrayList<>());



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

        latticeAcceptor = new LatticeAcceptor(this);
        latticeProposer = new LatticeProposer(this);
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

    public void receiveMessageFLL(String ip, int port, Message message) {
        acknowledge(ip, port, message);
    }

    void acknowledge(String ip, int port, Message message) {
        this.sender.acknowledge(message, ip, port);
    }

    public void receiveAcknowledgement(String ip, int port, int ackedID) {
        Host sender = getHost(ip, port);
        Message original = this.sender.getMessage(sender, ackedID);
        if (original != null && original.type != MessageParser.ACK_ACK_PREFIX) {
            Message ackAckMessage = MessageParser.createAckAck(ackedID);
            this.sendMessageSL(ackAckMessage, sender);
        }
        this.sender.acknowledged(sender, ackedID);
    }

    public void sendMessageSL(Message message, Host host) {
        this.sender.sendMessageSL(message, host);
    }

    public void receiveMessagePL(String ip, int port, String content) {
        int senderID = getHost(ip,  port).getId();
    }

    Host getHost(String ip, int port) {
        for (Host host : hosts) {
            if (host.getIp().equals(ip) && host.getPort() == port) {
                return host;
            }
        }
        return null;
    }
    public void bestEffortBroadcast(Message message) {
        sender.bestEffortBroadCast(message);
    }

    public void decide(int round, Set<Integer> decision) {
        StringBuilder sb = new StringBuilder();
        for (int x : decision) {
            sb.append(x);
            sb.append(' ');
        }
        sb.deleteCharAt(sb.length() - 1);

        while (logs.size() <= round) {
            logs.add(null);
        }
        logs.set(round, sb.toString());
    }
}
