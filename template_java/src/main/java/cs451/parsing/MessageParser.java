package cs451.parsing;

import java.util.HashSet;
import java.util.Set;

public class MessageParser {
    public static int parseLatticeAck(String message) {
        return Integer.parseInt(message.substring(4));
    }

    public static String createSetString(Set<Integer> s) {
        StringBuilder sb = new StringBuilder();
        for (int x : s) {
            sb.append(x);
            sb.append(' ');
        }
        return sb.toString();
    }

    public static Set<Integer> createStringSet(String s) {
        Set<Integer> set = new HashSet<>();
        for (String x : s.split(" ")) {
            set.add(Integer.parseInt(x));
        }
        return set;
    }

    public static MyPair parseLatticeNack(String message) {
        String[] parts = message.split(";");
        int proposal_number = Integer.parseInt(parts[0].substring(5));
        Set<Integer> accepted_value = createStringSet(parts[1]);
        return new MyPair(proposal_number, accepted_value);
    }

    public static MyPair parseLatticeProposal(String message) {
        String[] parts = message.substring(9).split(";");
        Set<Integer> proposed_value = createStringSet(parts[0]);
        int proposal_number = Integer.parseInt(parts[1]);
        return new MyPair(proposal_number, proposed_value);
    }
}
