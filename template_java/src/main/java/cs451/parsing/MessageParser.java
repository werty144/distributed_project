package cs451.parsing;

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
    public static byte[] createAck(byte[] message) {
        ByteBuffer bb = ByteBuffer.allocate(1 + message.length);
        bb.put(ACK_PREFIX);
        bb.put(message);
        return bb.array();
    }

    public static byte[] createLatticeAck(int round_number, int proposal_number) {
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.put(LATTICE_ACK_PREFIX);
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        return bb.array();
    }

    public static MyTriple parseLatticeAck(byte[] message) {
        int round_number = getInt(message, 1);
        int proposal_number = getInt(message, 5);
        return new MyTriple(round_number, proposal_number, null);
    }

    static int getInt(byte[] array, int index) {
        return ByteBuffer.wrap(Arrays.copyOfRange(array, index, index + 4)).getInt();
    }

    public static byte[] createLatticeNack(int round_number, int proposal_number, Set<Integer> accepted_value) {
        ByteBuffer bb = ByteBuffer.allocate(13 + 4 * accepted_value.size());
        bb.put(LATTICE_NACK_PREFIX);
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        bb.putInt(accepted_value.size());
        for (int x : accepted_value) {
            bb.putInt(x);
        }
        return bb.array();
    }

    public static MyTriple parseLatticeNack(byte[] message) {
        int round_number = getInt(message, 1);
        int proposal_number = getInt(message, 5);
        int set_size = getInt(message, 9);
        Set<Integer> accepted_value = new HashSet<>();
        for (int i = 0; i < set_size; i++) {
            accepted_value.add(getInt(message, 13 + 4 * i));
        }
        return new MyTriple(round_number, proposal_number, accepted_value);
    }

    public static byte[] createLatticeProposal(int round_number, int proposal_number, Set<Integer> proposed_value) {
        ByteBuffer bb = ByteBuffer.allocate(13 + 4 * proposed_value.size());
        bb.put(LATTICE_PROPOSAL_PREFIX);
        bb.putInt(round_number);
        bb.putInt(proposal_number);
        bb.putInt(proposed_value.size());
        for (int x: proposed_value) {
            bb.putInt(x);
        }
        return bb.array();
    }

    public static MyTriple parseLatticeProposal(byte[] message) {
        int round_number = getInt(message, 1);
        int proposal_number = getInt(message, 5);
        int set_size = getInt(message, 9);
        Set<Integer> proposed_value = new HashSet<>();
        for (int i = 0; i < set_size; i++) {
            proposed_value.add(getInt(message, 13 + 4 * i));
        }
        return new MyTriple(round_number, proposal_number, proposed_value);
    }

    public static ArrayList<byte[]> getIndividualMessages(byte[] message) {
        int lastSeptum = -1;
        ArrayList<byte[]> messages = new ArrayList<>();
        for (int i = 0; i < message.length; i++) {
            if (message[i] == (byte)'&') {
                messages.add(Arrays.copyOfRange(message, lastSeptum + 1, i));
                lastSeptum = i;
            }
        }
        return messages;
    }
}
