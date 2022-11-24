package cs451.hosting;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;


public class Receiver extends Thread {
    private DatagramSocket UDPSocket;
    private byte[] buf;
    private DatagramPacket packet;
    private Set<String> PLMessages = new HashSet<>();
    private Server server;
    private Set<String> URBDelivered = new HashSet<>();
    private Set<String> URBPending = new HashSet<>();
    private Map<String, BitSet> URBack = new HashMap<>();
    private Map<Integer, Integer> FIFOLastDelivered = new HashMap<>();
    private Map<Integer, List<Integer>> FIFOPending = new HashMap<>();
    private Map<InetAddress, String> InetAddressToIP = new HashMap<>();
    public Receiver(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        buf = new byte[256];
        packet = new DatagramPacket(buf, buf.length);
        for (Host host : server.hosts) {
            FIFOPending.put(host.getId(), new ArrayList<>());
            try {
                InetAddress address = InetAddress.getByName(host.getIp());
                InetAddressToIP.put(address, host.getIp());
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }

        }
    }

    public void log_stats() {
        System.out.println("PLMessages: " + PLMessages.size());
        System.out.println("URBDelivered: " + URBDelivered.size());
        System.out.println("URBPending: " + URBPending.size());
        System.out.println("URBAck keys: " + URBack.keySet().size());
        System.out.println("FIFOPending: " + FIFOPending.size());
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
        String ip = InetAddressToIP.get(packet.getAddress());
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

    private void receivePL(String ip, Integer port, String content) {
        String uniqueID = ip + '$' + port + '$' + content;
        if (!PLMessages.contains(uniqueID)) {
            PLMessages.add(uniqueID);
            server.receiveMessagePL(ip, port, content);
        }
    }

    private void receiveBEB(String ip, Integer port, String content) {
        if (URBDelivered.contains(content)) return;

        if (!URBack.containsKey(content)) {
            URBack.put(content, new BitSet(server.hosts.size()));
        }
        int senderID = server.getHost(ip, port).getId();
        URBack.get(content).set(senderID - 1, true);
        receiveURB(content);

        if (!URBPending.contains(content)) {
            URBPending.add(content);
            server.bestEffortBroadcast(content);
        }
    }

    private void receiveURB(String message) {
        if (URBDelivered.contains(message)) return;

        if (numberOfOnes(URBack.get(message)) > server.hosts.size() / 2) {
            deliverURB(message);
        }
    }

    private int numberOfOnes(BitSet bs) {
        int counter = 0;
        for (int i = 0; i < bs.size(); i++) {
            if (bs.get(i)) {
                counter += 1;
            }
        }
        return counter;
    }

    private void deliverURB(String message) {
        URBDelivered.add(message);
        URBack.remove(message);
        URBPending.remove(message);
        receiveFIFO(message);
    }

    private void receiveFIFO(String message) {
        String[] data = message.split(";");
        Integer senderID = Integer.parseInt(data[0]);
        int payload = Integer.parseInt(data[1]);
        FIFOPending.get(senderID).add(payload);

        if (!FIFOLastDelivered.containsKey(senderID)) {
            FIFOLastDelivered.put(senderID, 0);
        }

        if (FIFOLastDelivered.get(senderID) == payload - 1) {
            List<Integer> messagesFromSender = FIFOPending.get(senderID).stream().
                    sorted().
                    collect(Collectors.toList());
            Integer lastDeliveredTimeStamp = FIFOLastDelivered.get(senderID);
            for (Integer msg : messagesFromSender) {
                if (msg == lastDeliveredTimeStamp + 1) {
                    deliverFIFO(senderID, msg);
                    lastDeliveredTimeStamp++;
                } else {
                    break;
                }
            }
        }
    }

    private void deliverFIFO(Integer senderID, Integer message) {
        FIFOPending.get(senderID).remove(message);
        FIFOLastDelivered.put(senderID, message);
        server.deliverFIFO(senderID, message);
    }
}

