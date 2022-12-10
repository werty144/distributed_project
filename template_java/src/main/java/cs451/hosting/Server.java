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
    public List<String> logs = new ArrayList<>();



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

    public void receiveMessageFLL(String ip, int port, byte[] message) {
        acknowledge(ip, port, message);
    }

    void acknowledge(String ip, int port, byte[] message) {
        byte[] ack = MessageParser.createAck(message);
        this.sender.sendMessageFLL(ack, ip, port);
    }

    public void receiveAcknowledgement(String ip, int port, byte[] message) {
        Host sender = getHost(ip, port);
        this.sender.acknowledged(sender, message);
    }

    public void sendMessageSL(byte[] message, Host host) {
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
    public void bestEffortBroadcast(byte[] message) {
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
