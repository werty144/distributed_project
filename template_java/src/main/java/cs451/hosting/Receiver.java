package cs451.hosting;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.stream.Collectors;


public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<String> PLMessages = new HashSet<>();
    private Server server;
    private Set<BEBMessage> URBDelivered = new HashSet<>();
    private Set<BEBMessage> URBPending = new HashSet<>();
    private Map<BEBMessage, Set<Integer>> URBack = new HashMap<>();
    private Map<Integer, Integer> FIFOLastDelivered = new HashMap<>();
    private List<FIFOMessage> FIFOPending = new ArrayList<>();
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
        receiveURB(message);

        if (!URBPending.contains(message)) {
            URBPending.add(message);
            server.bestEffortBroadcast(message);
        }
    }

    private void receiveURB(BEBMessage message) {
        if (URBDelivered.contains(message)) return;

        if (URBack.get(message).size() > server.hosts.size() / 2) {
            deliverURB(message);
        }
    }

    private void deliverURB(BEBMessage message) {
        URBDelivered.add(message);
        receiveFIFO(new FIFOMessage(message.SenderID, message.content, Integer.parseInt(message.content)));
    }

    private void receiveFIFO(FIFOMessage message) {
        FIFOPending.add(message);

        if (!FIFOLastDelivered.containsKey(message.senderID)) {
            FIFOLastDelivered.put(message.senderID, 0);
        }

        if (FIFOLastDelivered.get(message.senderID) == message.timestamp - 1) {
            List<FIFOMessage> messagesFromSender = FIFOPending.stream().
                    filter(it -> it.senderID.equals(message.senderID)).
                    sorted(new FIFOMessageComparator()).
                    collect(Collectors.toList());
            Integer lastDeliveredTimeStamp = FIFOLastDelivered.get(message.senderID);
            for (FIFOMessage msg : messagesFromSender) {
                if (msg.timestamp == lastDeliveredTimeStamp + 1) {
                    deliverFIFO(msg);
                    lastDeliveredTimeStamp++;
                } else {
                    break;
                }
            }
        }
    }

    private void deliverFIFO(FIFOMessage message) {
        FIFOPending.remove(message);
        FIFOLastDelivered.put(message.senderID, message.timestamp);
        server.deliverFIFO(message);
    }
}

