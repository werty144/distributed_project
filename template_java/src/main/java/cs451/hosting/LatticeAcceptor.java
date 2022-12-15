package cs451.hosting;

import cs451.Message;
import cs451.parsing.MessageParser;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.*;

public class LatticeAcceptor {
    Server server;
    List<Set<Integer>> accepted_values = new ArrayList<>();

    public LatticeAcceptor(Server server) {
        this.server = server;
    }

    public void receive_proposal(int round_number, Set<Integer> proposed_value, Integer proposal_number, Host sender) {
        while (accepted_values.size() <= round_number) {
            accepted_values.add(new HashSet<>());
        }
        Set<Integer> accepted_value = accepted_values.get(round_number);
        if (proposed_value.containsAll(accepted_value)) {
            accepted_values.set(round_number, proposed_value);
            sendAck(round_number, proposal_number, sender);
        } else {
            accepted_value.addAll(proposed_value);
            sendNack(round_number, proposal_number, sender);
        }
    }

    void sendAck(int round_number, Integer proposal_number, Host toWhom) {
        Message message = MessageParser.createLatticeAck(round_number, proposal_number);
        server.sendMessageSL(message, toWhom);
    }

    void sendNack(int round_number, Integer proposal_number, Host toWhom) {
        Message message = MessageParser.createLatticeNack(
                round_number,
                proposal_number,
                accepted_values.get(round_number)
        );
        server.sendMessageSL(message, toWhom);
    }
}
