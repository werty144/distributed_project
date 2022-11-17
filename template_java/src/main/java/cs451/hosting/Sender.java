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

    public Sender(DatagramSocket UDPSocket, Server server) {
        this.UDPSocket = UDPSocket;
        this.server = server;
        for (Host host : server.hosts) {
            receiversToMessages.put(
                    host.getId(),
                    Collections.synchronizedList(new ArrayList<>())
            );
            idsToHosts.put(host.getId(), host);
            lastMessageURB.put(host.getId(), 0);
        }
    }

    private void sendConcatenatedMessagesFLL(Integer maxConcatNumber) {
        for (Integer id : receiversToMessages.keySet()) {
            int messagesConcatenated = 0;
            StringBuilder concatenatedMessage = new StringBuilder();
            List<String> messages = receiversToMessages.get(id);
            synchronized (messages) {
                int messageBucketsSent = 0;
                for (String content : messages) {
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
            try {
                sleep(10);
            } catch (InterruptedException ignored) {

            }
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

    public void bestEffortBroadCast(BEBMessage message) {
        String content = message.SenderID.toString() + ';' + message.content;
        for (Host host : server.hosts) {
            sendMessageSL(new Message(content, server.getHost(), host));
        }
    }

    public void uniformReliableBroadcast(String content) {
//        Integer n_hosts = server.hosts.size();
//        int maxMessagesFlyingAtATime = max(20_000 / (n_hosts * n_hosts), 8);
//        while (SLMessages.size() > maxMessagesFlyingAtATime) {
//            try {
//                sleep(10);
//            } catch (InterruptedException ignored) {
//
//            }
//        }
        bestEffortBroadCast(new BEBMessage(server.getHost().getId(), content));
    }

    public void updateQueues(Integer m) {
        Integer n_hosts = server.hosts.size();
        int MAX_MESSAGES_IN_QUEUE = max(20_000 / (n_hosts * n_hosts * n_hosts), 8);
        while (true) {
            try {
                sleep(10);
            } catch (InterruptedException ignored) {}

            for (Host host : server.hosts) {
                List<String> messages = receiversToMessages.get(host.getId());
                synchronized (messages) {
                    if (messages.size() < MAX_MESSAGES_IN_QUEUE) {
                        Integer cur_value = lastMessageURB.get(host.getId());
                        if (cur_value >= m) continue;
                        String new_message = Integer.toString(cur_value + 1);
                        String content = Integer.toString(server.getHost().getId()) + ';' + new_message;
                        messages.add(content);
                        lastMessageURB.put(host.getId(), cur_value + 1);
                        server.FIFOBroadcasted(new_message);
                    }
                }
            }
        }
    }

    public void FIFOBroadcast(String content) {
        uniformReliableBroadcast(content);
    }
}
