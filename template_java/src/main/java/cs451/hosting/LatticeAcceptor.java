package cs451.hosting;

import cs451.parsing.MessageParser;

import java.util.HashSet;
import java.util.Set;

public class LatticeAcceptor {
    Server server;
    Set<Integer> accepted_value = new HashSet<>();

    public LatticeAcceptor(Server server) {
        this.server = server;
    }

    public void receive_proposal(Set<Integer> proposed_value, Integer proposal_number, Host sender) {
        if (proposed_value.containsAll(accepted_value)) {
            accepted_value = proposed_value;
            sendAck(proposal_number, sender);
        } else {
            accepted_value.addAll(proposed_value);
            sendNack(proposal_number, sender);
        }
    }

    void sendAck(Integer proposal_number, Host toWhom) {
        String message = "ACK$" + proposal_number;
        server.sendMessageFLL(message, toWhom);
    }

    void sendNack(Integer proposal_number, Host toWhom) {
        String message = "NACK$" + proposal_number + ";" + MessageParser.createSetString(accepted_value);
        server.sendMessageFLL(message, toWhom);
    }
}
