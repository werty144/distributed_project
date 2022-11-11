package cs451.hosting;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<String> PLMessages = new HashSet<>();
    private Server server;
    private Set<BEBMessage> URBdelivered = new HashSet<>();
    private Set<BEBMessage> URBPending = new HashSet<>();
    private Map<BEBMessage, Set<Integer>> URBack = new HashMap<>();
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
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

        String content = new String(packet.getData(), 0, packet.getLength());
        String ip = packet.getAddress().toString();
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }

        if (content.startsWith("ack$")) {
            server.receiveAcknowledgement(ip, packet.getPort(), content.substring(4));
            return;
        }

        for (String individualMessage : content.split("&")) {
            server.receiveMessageFLL(ip, packet.getPort(), individualMessage);
            receiveBEB(ip, packet.getPort(), individualMessage);
        }
    }

    private void receiveSL(String ip, Integer port, String content) {
        receivePL(ip, port, content);
    }

    private void receivePL(String ip, Integer port, String content) {
        String uniqueID = ip + '$' + port + '$' + content;
        if (!PLMessages.contains(uniqueID)) {
            PLMessages.add(uniqueID);
            server.receiveMessagePL(ip, port, content);
        }
    }

    private void receiveBEB(String ip, Integer port, String content) {
        String[] data = content.split(";");
        Integer originalSender = Integer.parseInt(data[0]);
        String payload = data[1];
        BEBMessage message = new BEBMessage(originalSender, payload);

        if (!URBack.containsKey(message)) {
            URBack.put(message, new HashSet<>());
        }
        Integer senderID = server.getHost(ip, port).getId();
        URBack.get(message).add(senderID);
        tryDeliverURB(message);

        if (!URBPending.contains(message)) {
            URBPending.add(message);
            server.bestEffortBroadcast(message);
        }
    }

    private void tryDeliverURB(BEBMessage message) {
        if (URBdelivered.contains(message)) return;

        if (URBack.get(message).size() > server.hosts.size() / 2) {
            URBdelivered.add(message);
            System.out.println("URB Delivered " + message.content + " from " + message.SenderID);
            server.URBDeliver(message);
        }
    }
}

