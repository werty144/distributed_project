package cs451.parsing;

import cs451.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MessageParser {
    public static byte ACK_PREFIX = 0;
    public static byte LATTICE_ACK_PREFIX = 1;
    public static byte LATTICE_NACK_PREFIX = 2;
    public static byte LATTICE_PROPOSAL_PREFIX = 3;
    public static byte LATTICE_DECIDED_PREFIX = 4;
    public static byte ACK_ACK_PREFIX = 5;
    public static Message createAck(int acked_id) {
        return new Message(ACK_PREFIX, ByteBuffer.allocate(4).putInt(acked_id).array());
    }

    public static Message createLatticeAck(int round_number, int proposal_number) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        return new Message(LATTICE_ACK_PREFIX, bb.array());
    }

    public static Message createDecided(int round_number) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(round_number);
        return new Message(LATTICE_DECIDED_PREFIX, bb.array());
    }

    public static int getDecidedRound(Message message) {
        return getInt(message.content, 0);
    }

    public static int getAckedID(Message message) {
        return getInt(message.content, 0);
    }

    public static Message createAckAck(int originalID) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(originalID);
        return new Message(ACK_ACK_PREFIX, bb.array());
    }

    public static int getAckAckID(Message message) {
        return getInt(message.content, 0);
    }

    public static MyTriple parseLatticeAck(Message message) {
        assert (message.type == LATTICE_ACK_PREFIX);
        int round_number = getInt(message.content, 0);
        int proposal_number = getInt(message.content, 4);
        return new MyTriple(round_number, proposal_number, null);
    }

    public static int getInt(byte[] array, int index) {
        return ByteBuffer.wrap(Arrays.copyOfRange(array, index, index + 4)).getInt();
    }

    public static Message createLatticeNack(int round_number, int proposal_number, Set<Integer> accepted_value) {
        ByteBuffer bb = ByteBuffer.allocate(12 + 4 * accepted_value.size());
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        bb.putInt(accepted_value.size());
        for (int x : accepted_value) {
            bb.putInt(x);
        }
        return new Message(LATTICE_NACK_PREFIX, bb.array());
    }

    public static MyTriple parseLatticeNack(Message message) {
        assert (message.type == LATTICE_NACK_PREFIX);
        int round_number = getInt(message.content, 0);
        int proposal_number = getInt(message.content, 4);
        int set_size = getInt(message.content, 8);
        Set<Integer> accepted_value = new HashSet<>();
        for (int i = 0; i < set_size; i++) {
            accepted_value.add(getInt(message.content, 12 + 4 * i));
        }
        return new MyTriple(round_number, proposal_number, accepted_value);
    }

    public static Message createLatticeProposal(int round_number, int proposal_number, Set<Integer> proposed_value) {
        ByteBuffer bb = ByteBuffer.allocate(12 + 4 * proposed_value.size());
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        bb.putInt(proposed_value.size());
        for (int x: proposed_value) {
            bb.putInt(x);
        }
        return new Message(LATTICE_PROPOSAL_PREFIX, bb.array());
    }

    public static MyTriple parseLatticeProposal(Message message) {
        assert (message.type == LATTICE_PROPOSAL_PREFIX);
        int round_number = getInt(message.content, 0);
        int proposal_number = getInt(message.content, 4);
        int set_size = getInt(message.content, 8);
        Set<Integer> proposed_value = new HashSet<>();
        for (int i = 0; i < set_size; i++) {
            proposed_value.add(getInt(message.content, 12 + 4 * i));
        }
        return new MyTriple(round_number, proposal_number, proposed_value);
    }

    public static ArrayList<Message> getIndividualMessages(byte[] raw) {
        int lastMessageOffset = 0;
        ArrayList<Message> messages = new ArrayList<>();
        while (lastMessageOffset < raw.length) {
            int curSize = getInt(raw, lastMessageOffset);
            byte[] raw_message = Arrays.copyOfRange(raw, lastMessageOffset + 4, lastMessageOffset + curSize + 4);
            Message message = Message.fromBytes(raw_message);
            messages.add(message);
            lastMessageOffset = lastMessageOffset + curSize + 4;
        }
        return messages;
    }
}
