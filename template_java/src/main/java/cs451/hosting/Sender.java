package cs451.hosting;

import cs451.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Sender extends Thread {
    private DatagramSocket UDPSocket;
    private Server server;
    private final List<Message> SLMessages = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, LinkedList<String>> receiversToMessages = new HashMap<>();
    private final Map<Integer, Host> idsToHosts = new HashMap<>();
    public Sender(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
    }

    private void sortMessages() {
        synchronized (SLMessages) {
            for (Message message : SLMessages) {
                if (!receiversToMessages.containsKey(message.getReceiver().getId())) {
                    receiversToMessages.put(message.getReceiver().getId(), new LinkedList<String>());
                    idsToHosts.put(message.getReceiver().getId(), message.getReceiver());
                }
                receiversToMessages.get(message.getReceiver().getId()).add(message.getContent());
            }
        }
    }

    private void sendConcatenatedMessagesFLL(Integer maxConcatNumber) {
        for (Integer id : receiversToMessages.keySet()) {
            int messagesConcatenated = 0;
            StringBuilder concatenatedMessage = new StringBuilder();
            LinkedList<String> messages = receiversToMessages.get(id);
            int messageBucketsSent = 0;
            while (!messages.isEmpty()) {
                if (messagesConcatenated == maxConcatNumber) {
                    Message message = new Message(
                            concatenatedMessage.toString(),
                            server.getHost(),
                            idsToHosts.get(id)
                    );
                    sendMessageFLL(message);
                    messageBucketsSent += 1;
                    messagesConcatenated = 0;
                    concatenatedMessage.setLength(0);
                }
                concatenatedMessage.append(messages.remove()).append("&");
                messagesConcatenated += 1;

                if (messageBucketsSent == 100) {
                    try {
                        sleep(10);
                    } catch (InterruptedException ignored) {

                    }
                    messageBucketsSent = 0;
                }
            }

            if (concatenatedMessage.length() > 0) {
                Message message = new Message(
                        concatenatedMessage.toString(),
                        server.getHost(),
                        idsToHosts.get(id)
                );
                sendMessageFLL(message);
            }
        }
    }

    public void run() {
        while (true) {
            sortMessages();
            sendConcatenatedMessagesFLL(8);
        }
    }

    public void sendMessageFLL(Message message) {
        byte[] buf = message.getContent().getBytes();
        InetAddress address;
        try {
            address = InetAddress.getByName(message.getReceiver().getIp());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        DatagramPacket packet = new DatagramPacket(
                buf,
                buf.length,
                address,
                message.getReceiver().getPort()
        );
        try {
            UDPSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageSL(Message message) {
        SLMessages.add(message);
    }

    public void sendMessagePL(Message message) {
        sendMessageSL(message);
    }

    public void acknowledged(Host receiver, String content) {
        synchronized (SLMessages) {
            SLMessages.removeIf(m ->
                    (m.getReceiver().getId() == receiver.getId()) &&
                            m.getContent().equals(content)
            );
        }
    }
}
