package cs451;

import cs451.hosting.Host;

public class Message {
    private Host sender;
    private Host receiver;
    private String content;

    public Message(String content, Host sender, Host receiver) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
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
}