package cs451;

import cs451.hosting.Host;

public class Message {
    private Host sender;
    private Host receiver;
    private String content;
    private int id;

    public Message(String content, Host sender, Host receiver, int id) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.id = id;
    }

    public Host getReceiver() {
        return receiver;
    }

    public Host getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public int getId() {
        return id;
    }
}
