package cs451.hosting;
import cs451.Message;
import cs451.parsing.MessageParser;
import cs451.parsing.MyTriple;

import java.awt.image.AreaAveragingScaleFilter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;


//class PLMessage {
//    byte[] content;
//    public PLMessage(byte[] content) {
//        this.content = content;
//    }
//
//    @Override
//    public int hashCode() {
//        return Arrays.hashCode(content);
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (!(obj instanceof PLMessage)) return false;
//        return Arrays.equals(((PLMessage) obj).content, content);
//    }
//}

public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private HashMap<Integer, Set<Integer>> PLMessages = new HashMap<>();
    private Server server;

    private Map<InetAddress, String> InetAddressToIP = new HashMap<>();
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[32936];
        packet = new DatagramPacket(buf, buf.length);
        for (Host host : server.hosts) {
            try {
                InetAddress address = InetAddress.getByName(host.getIp());
                InetAddressToIP.put(address, host.getIp());
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
            PLMessages.put(host.getId(), new HashSet<>());
        }
    }

    public void run() {
        while (true) {
            receiveRawFLL();
        }
    }

    private void receiveRawFLL() {
        try {
            UDPSocket.receive(packet);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        byte[] content = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
        String ip = InetAddressToIP.get(packet.getAddress());
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }

        for (Message individualMessage : MessageParser.getIndividualMessages(content)) {
            receiveFLL(individualMessage, ip, packet.getPort());
        }
    }

    private void receiveFLL(Message message, String ip, int port) {
        if (message.type == MessageParser.ACK_PREFIX) {
            int ackedID = MessageParser.getAckedID(message);
            server.receiveAcknowledgement(
                    ip,
                    port,
                    ackedID
            );
            return;
        }
        if (message.type == MessageParser.ACK_ACK_PREFIX) {
            int originalID = MessageParser.getAckAckID(message);
            PLMessages.get(server.getHost(ip, port).getId()).remove(originalID);
            server.acknowledge(ip, port, message);
            return;
        }
        server.receiveMessageFLL(ip, port, message);
        receivePL(message, ip, port);
    }

    private void receivePL(Message message, String ip, int port) {
        int senderID = server.getHost(ip, port).getId();
        if (PLMessages.get(senderID).contains(message.id)) {
            return;
        }
        PLMessages.get(senderID).add(message.id);

        if (message.type == MessageParser.LATTICE_ACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeAck(message);
            server.latticeProposer.receive_ack(ans.round_number, ans.proposal_number);
        }

        if (message.type == MessageParser.LATTICE_NACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeNack(message);
            server.latticeProposer.receive_nack(ans.round_number, ans.proposal_number, ans.value);
        }

        if (message.type == MessageParser.LATTICE_PROPOSAL_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeProposal(message);
            server.latticeAcceptor.receive_proposal(
                    ans.round_number,
                    ans.value,
                    ans.proposal_number,
                    server.getHost(ip, port)
            );
        }

        if (message.type == MessageParser.LATTICE_DECIDED_PREFIX) {
            int decidedRound = MessageParser.getDecidedRound(message);
            server.latticeAcceptor.hostDecided(decidedRound, senderID);
        }
    }
}

