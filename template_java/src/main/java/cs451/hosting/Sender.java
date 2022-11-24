package cs451.hosting;

import cs451.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static java.lang.Math.max;


public class Sender extends Thread {
    private DatagramSocket UDPSocket;
    private Server server;
    private final Map<Integer, List<String>> receiversToMessages = new HashMap<>();
    private final Map<Integer, Host> idsToHosts = new HashMap<>();
    private final Map<Integer, Integer> lastMessageURB = new HashMap<>();
    private final Map<Integer, Integer> lastMessageBEB = new HashMap<>();
    private final List<String> messagesToBEB = Collections.synchronizedList(new ArrayList<>());
    int MAX_MESSAGES_IN_QUEUE;
    int FIFOTotal = -1;

    Map <String, InetAddress> inetAddress = new HashMap<>();

    public Sender(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        Integer n_hosts = server.hosts.size();
        this.MAX_MESSAGES_IN_QUEUE = max(20_000 / (n_hosts * n_hosts * n_hosts), 8);
        for (Host host : server.hosts) {
            receiversToMessages.put(
                    host.getId(),
                    Collections.synchronizedList(new ArrayList<>())
            );
            idsToHosts.put(host.getId(), host);
            lastMessageURB.put(host.getId(), 0);
            lastMessageBEB.put(host.getId(), -1);

            try {
                inetAddress.put(host.getIp(), InetAddress.getByName(host.getIp()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void putFIFOTotal(int m) {
        FIFOTotal = m;
    }

    private void sendConcatenatedMessagesFLL(Integer maxConcatNumber) {
        for (Integer id : receiversToMessages.keySet()) {
            Host receiver = idsToHosts.get(id);
            int messagesConcatenated = 0;
            StringBuilder concatenatedMessage = new StringBuilder();
            List<String> messages = receiversToMessages.get(id);
            synchronized (messages) {
                int messageBucketsSent = 0;
                for (String content : messages) {
                    if (messagesConcatenated == maxConcatNumber) {
                        sendMessageFLL(concatenatedMessage.toString(), receiver.getIp(), receiver.getPort());
                        messageBucketsSent += 1;
                        messagesConcatenated = 0;
                        concatenatedMessage.setLength(0);
                    }
                    concatenatedMessage.append(content).append("&");
                    messagesConcatenated += 1;

                    if (messageBucketsSent == 100) {
                        try {
                            sleep(10);
                        } catch (InterruptedException ignored) {

                        }
                        messageBucketsSent = 0;
                    }
                }
            }

            if (concatenatedMessage.length() > 0) {
                sendMessageFLL(concatenatedMessage.toString(), receiver.getIp(), receiver.getPort());
            }
        }
    }

    private void log_stats() {
        int total_messages = 0;
        for (Integer id : receiversToMessages.keySet()) {
            total_messages += receiversToMessages.get(id).size();
        }
    }

    public void run() {
        while (true) {
            try {
                sleep(10);
            } catch (InterruptedException ignored) {

            }
            updateQueues();
            sendConcatenatedMessagesFLL(8);
        }
    }

    public void sendMessageFLL(String content, String ip, int port) {
        byte[] buf = content.getBytes();
        InetAddress address = inetAddress.get(ip);
        DatagramPacket packet = new DatagramPacket(
                buf,
                buf.length,
                address,
                port
        );
        try {
            UDPSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageSL(Message message) {
        List<String> messages = receiversToMessages.get(message.getReceiver().getId());
        synchronized (messages) {
            messages.add(message.getContent());
        }
    }

    public void sendMessagePL(Message message) {
        sendMessageSL(message);
    }


    public void acknowledged(Host receiver, String content) {
        List<String> messages = receiversToMessages.get(receiver.getId());
        synchronized (messages) {
            messages.removeIf(m -> m.equals(content));
        }
    }

    public void bestEffortBroadCast(String message) {
        synchronized (messagesToBEB) {
            messagesToBEB.add(message);
        }
    }

//    public void uniformReliableBroadcast(String content) {
//        bestEffortBroadCast(new BEBMessage(server.getHost().getId(), content));
//    }

    public void updateQueues() {
        for (Host host : server.hosts) {
            List<String> messages = receiversToMessages.get(host.getId());
            synchronized (messages) {

                synchronized (messagesToBEB) {
                    int nextBEBMessage = lastMessageBEB.get(host.getId()) + 1;
                    while ((messages.size() < MAX_MESSAGES_IN_QUEUE) && (nextBEBMessage < messagesToBEB.size())) {
                        messages.add(messagesToBEB.get(nextBEBMessage));
                        nextBEBMessage += 1;
                    }
                    lastMessageBEB.put(host.getId(), nextBEBMessage - 1);
                }

                int nextURBMessage = lastMessageURB.get(host.getId()) + 1;
                while ((messages.size() < MAX_MESSAGES_IN_QUEUE) && (nextURBMessage <= FIFOTotal)) {
                    String new_message = Integer.toString(nextURBMessage);
                    String content = Integer.toString(server.getHost().getId()) + ';' + new_message;
                    messages.add(content);
                    server.FIFOBroadcasted(new_message);
                    nextURBMessage += 1;
                }
                lastMessageURB.put(host.getId(), nextURBMessage - 1);
            }
        }
    }

//    public void FIFOBroadcast(String content) {
//        uniformReliableBroadcast(content);
//    }
}
