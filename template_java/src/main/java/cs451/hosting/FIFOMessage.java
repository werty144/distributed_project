package cs451.hosting;

import java.util.Comparator;

public class FIFOMessage {
    public Integer senderID;
    public String content;
    public Integer timestamp;

    public FIFOMessage(Integer senderID, String content, Integer timestamp) {
        this.senderID = senderID;
        this.content = content;
        this.timestamp = timestamp;
    }
}

