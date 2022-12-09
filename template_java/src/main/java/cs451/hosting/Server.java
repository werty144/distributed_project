package cs451.hosting;

import cs451.Message;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

import static java.lang.Math.max;


//class Pair<T1, T2> {
//    T1 first;
//    T2 second;
//
//    public Pair(T1 fst, T2 snd) {
//        first = fst;
//        second = snd;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (getClass() != obj.getClass()) return false;
//        Pair<T1, T2> other = (Pair<T1, T2>) obj;
//        return other.first.equals(first) && other.second.equals(second);
//    }
//
//    @Override
//    public int hashCode() {
//        return (first.toString() + '$' + second.toString()).hashCode();
//    }
//}

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

    public void receiveMessageFLL(String ip, int port, String message) {
        acknowledge(ip, port, message);
    }

    void acknowledge(String ip, int port, String message) {
        this.sender.sendMessageFLL("ack$" + message, ip, port);
    }

    public void receiveAcknowledgement(String ip, int port, String message) {
        Host sender = getHost(ip, port);
        this.sender.acknowledged(sender, message);
    }

    public void sendMessageFLL(String message, Host host) {
        this.sender.sendMessageFLL(message, host.getIp(), host.getPort());
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
    public void bestEffortBroadcast(String message) {
        sender.bestEffortBroadCast(message);
    }

    public void receiveLatticeACK(int proposal_number) {
        latticeProposer.receive_ack(proposal_number);
    }

    public void receiveLatticeNACK(int proposal_number, Set<Integer> accepted_value) {
        latticeProposer.receive_nack(proposal_number, accepted_value);
    }

    public void receiveLatticeProposal(Set<Integer> proposed_value, int proposal_number, Host sender) {
        latticeAcceptor.receive_proposal(proposed_value, proposal_number, sender);
    }

    public void latticeProposal(Set<Integer> proposed_value) {
        latticeProposer.propose(proposed_value);
    }
}
