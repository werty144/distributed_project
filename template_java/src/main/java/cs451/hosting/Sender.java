package cs451.hosting;

import cs451.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Sender extends Thread {
    private DatagramSocket UDPSocket;
    private ArrayList<Message> SLMessages = new ArrayList<>();
    public Sender(DatagramSocket UDPSocket) {
        this.UDPSocket = UDPSocket;
    }

    public void run() {
        while (true) {
//            try {
//                Thread.sleep(1000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            for (Message message: SLMessages) {
                sendMessageFLL(message);
            }
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
}
