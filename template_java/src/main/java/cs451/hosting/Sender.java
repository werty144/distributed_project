package cs451.hosting;

import cs451.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static java.lang.Math.max;


public class Sender extends Thread {
    private DatagramSocket UDPSocket;
    private Server server;
    private final Map<Integer, List<byte[]>> receiversToMessages = new HashMap<>();
    private final Map<Integer, Host> idsToHosts = new HashMap<>();
    private final Map<Integer, Integer> lastMessageBEB = new HashMap<>();
    private final List<byte[]> messagesToBEB = Collections.synchronizedList(new LinkedList<>());
    int MAX_MESSAGES_IN_QUEUE;

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
            lastMessageBEB.put(host.getId(), -1);

            try {
                inetAddress.put(host.getIp(), InetAddress.getByName(host.getIp()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void sendConcatenatedMessagesFLL(Integer maxConcatNumber) {
        for (Integer id : receiversToMessages.keySet()) {
            Host receiver = idsToHosts.get(id);
            int messagesConcatenated = 0;
            ByteArrayOutputStream concatenatedMessage = new ByteArrayOutputStream();
            List<byte[]> messages = receiversToMessages.get(id);
            synchronized (messages) {
                int messageBucketsSent = 0;
                for (byte[] content : messages) {
                    if (messagesConcatenated == maxConcatNumber) {
                        sendMessageFLL(concatenatedMessage.toByteArray(), receiver.getIp(), receiver.getPort());
                        messageBucketsSent += 1;
                        messagesConcatenated = 0;
                        concatenatedMessage.reset();
                    }
                    try {
                        concatenatedMessage.write(content);
                        concatenatedMessage.write((byte) '&');
                    } catch (IOException ignored) {}
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

            if (concatenatedMessage.size() > 0) {
                sendMessageFLL(concatenatedMessage.toByteArray(), receiver.getIp(), receiver.getPort());
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
            cleanMessagesToBEB();
        }
    }

    private void cleanMessagesToBEB() {
        int curMin = Integer.MAX_VALUE;
        for (Integer v : lastMessageBEB.values()) {
            if (v < curMin) curMin = v;
        }
        int valuesToClean = curMin + 1;
        synchronized (messagesToBEB) {
            if (valuesToClean > 0) {
                messagesToBEB.subList(0, valuesToClean).clear();
            }
        }

        synchronized (lastMessageBEB) {
            lastMessageBEB.replaceAll((k, v) -> v - valuesToClean);
        }
    }

    public void sendMessageFLL(byte[] content, String ip, int port) {
        InetAddress address = inetAddress.get(ip);
        DatagramPacket packet = new DatagramPacket(
                content,
                content.length,
                address,
                port
        );
        try {
            UDPSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageSL(byte[] message, Host receiver) {
        List<byte[]> messages = receiversToMessages.get(receiver.getId());
        synchronized (messages) {
            messages.add(message);
        }
    }


    public void acknowledged(Host receiver, byte[] content) {
        List<byte[]> messages = receiversToMessages.get(receiver.getId());
        synchronized (messages) {
            messages.removeIf(m -> Arrays.equals(m, content));
        }
    }

    public void bestEffortBroadCast(byte[] message) {
        synchronized (messagesToBEB) {
            messagesToBEB.add(message);
        }
    }

    public void updateQueues() {
        for (Host host : server.hosts) {
            List<byte[]> messages = receiversToMessages.get(host.getId());
            synchronized (messages) {

                synchronized (messagesToBEB) {
                    int nextBEBMessage = lastMessageBEB.get(host.getId()) + 1;
                    while ((messages.size() < MAX_MESSAGES_IN_QUEUE) && (nextBEBMessage < messagesToBEB.size())) {
                        messages.add(messagesToBEB.get(nextBEBMessage));
                        nextBEBMessage += 1;
                    }
                    lastMessageBEB.put(host.getId(), nextBEBMessage - 1);
                }
            }
        }
    }
}
