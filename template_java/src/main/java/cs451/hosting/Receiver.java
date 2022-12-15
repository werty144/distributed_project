package cs451.hosting;
import cs451.parsing.MessageParser;
import cs451.parsing.MyTriple;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;


class PLMessage {
    byte[] content;
    public PLMessage(byte[] content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PLMessage)) return false;
        return Arrays.equals(((PLMessage) obj).content, content);
    }
}

public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<PLMessage> PLMessages = new HashSet<>();
    private Server server;

    private Map<InetAddress, String> InetAddressToIP = new HashMap<>();
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[32904];
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

        for (byte[] individualMessage : MessageParser.getIndividualMessages(content)) {
            receiveFLL(individualMessage, ip, packet.getPort());
        }
    }

    private void receiveFLL(byte[] message, String ip, int port) {
        if (message[0] == MessageParser.ACK_PREFIX) {
            server.receiveAcknowledgement(
                    ip,
                    port,
                    Arrays.copyOfRange(message, 1, message.length)
            );
            return;
        }
        server.receiveMessageFLL(ip, port, message);
        receivePL(message, ip, port);
    }

    private void receivePL(byte[] message, String ip, int port) {
        ByteBuffer bb = ByteBuffer.allocate(message.length + 4);
        bb.putInt(server.getHost(ip, port).getId());
        bb.put(Arrays.copyOf(message, message.length));
        PLMessage plMessage = new PLMessage(bb.array());
        if (PLMessages.contains(plMessage)) {
            return;
        }
        PLMessages.add(plMessage);

        if (message[0] == MessageParser.LATTICE_ACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeAck(message);
            server.latticeProposer.receive_ack(ans.round_number, ans.proposal_number);
        }

        if (message[0] == MessageParser.LATTICE_NACK_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeNack(message);
            server.latticeProposer.receive_nack(ans.round_number, ans.proposal_number, ans.value);
        }

        if (message[0] == MessageParser.LATTICE_PROPOSAL_PREFIX) {
            MyTriple ans = MessageParser.parseLatticeProposal(message);
            server.latticeAcceptor.receive_proposal(
                    ans.round_number,
                    ans.value,
                    ans.proposal_number,
                    server.getHost(ip, port)
            );
        }
    }
}

