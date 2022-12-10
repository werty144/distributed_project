package cs451.hosting;
import cs451.parsing.MessageParser;
import cs451.parsing.MyTriple;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;


public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<String> PLMessages = new HashSet<>();
    private Server server;

    private Map<InetAddress, String> InetAddressToIP = new HashMap<>();
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[2048];
        packet = new DatagramPacket(buf, buf.length);
        for (Host host : server.hosts) {
            try {
                InetAddress address = InetAddress.getByName(host.getIp());
                InetAddressToIP.put(address, host.getIp());
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        }
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

        byte[] content = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
        String ip = InetAddressToIP.get(packet.getAddress());
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }

        if (content[0] == MessageParser.ACK_PREFIX) {
            server.receiveAcknowledgement(
                    ip,
                    packet.getPort(),
                    Arrays.copyOfRange(content, 1, content.length)
            );
            return;
        }

        for (byte[] individualMessage : MessageParser.getIndividualMessages(content)) {
            server.receiveMessageFLL(ip, packet.getPort(), individualMessage);
            receiveBEB(ip, packet.getPort(), individualMessage);
        }
    }

    private void receivePL(String ip, Integer port, String content) {
        String uniqueID = ip + '$' + port + '$' + content;
        if (!PLMessages.contains(uniqueID)) {
            PLMessages.add(uniqueID);
            server.receiveMessagePL(ip, port, content);
        }
    }

    private void receiveBEB(String ip, Integer port, byte[] content) {

        int senderID = server.getHost(ip, port).getId();
        if (content[0] == MessageParser.LATTICE_ACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeAck(content);
            server.latticeProposer.receive_ack(ans.round_number, ans.proposal_number);
        }

        if (content[0] == MessageParser.LATTICE_NACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeNack(content);
            server.latticeProposer.receive_nack(ans.round_number, ans.proposal_number, ans.value);
        }

        if (content[0] == MessageParser.LATTICE_PROPOSAL_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeProposal(content);
            server.latticeAcceptor.receive_proposal(
                    ans.round_number,
                    ans.value,
                    ans.proposal_number,
                    server.getHost(ip, port)
            );
        }
    }
}

