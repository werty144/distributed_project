package cs451;

import cs451.hosting.Host;
import cs451.parsing.MessageParser;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {
    static int spareID = 0;
    public int id;
    public byte type;
    public byte[] content;

    public Message(byte type, byte[] content) {
        id = spareID++;
        this.type = type;
        this.content = content;
    }

    public Message(int id, byte type, byte[] content) {
        this.id = id;
        this.type = type;
        this.content = content;
    }

    public byte[] getBytes() {
        ByteBuffer bb = ByteBuffer.allocate(5 + content.length);
        bb.putInt(id);
        bb.put(type);
        bb.put(content);
        return bb.array();
    }

    public byte[] getBytesWithLength() {
        ByteBuffer bb = ByteBuffer.allocate(9 + content.length);
        bb.putInt(9 + content.length);
        bb.putInt(id);
        bb.put(type);
        bb.put(content);
        return bb.array();
    }

    public static Message fromBytes(byte[] raw) {
        int id = MessageParser.getInt(raw, 0);
        byte type = raw[4];
        byte[] content = Arrays.copyOfRange(raw, 5, raw.length);
        return new Message(id, type, content);
    }
}
