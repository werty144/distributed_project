package cs451.hosting;

import cs451.parsing.MessageParser;

import java.util.Set;

public class LatticeProposer {
    boolean active = false;
    int ack_count = 0;
    int nack_count = 0;
    int active_proposal_number = 0;
    Set<Integer> proposed_value = null;
    Server server;
    int f;

    public LatticeProposer(Server server) {
        this.server = server;
        f = (server.hosts.size() - 1) / 2;
    }

    public void propose(Set<Integer> proposal) {
        System.out.println("Proposing " + proposal);
        proposed_value = proposal;
        active = true;
        active_proposal_number++;
        ack_count = 0;
        nack_count = 0;
        broadcast_value();
    }

    public void receive_ack(int proposal_number) {
        if (proposal_number == active_proposal_number) {
            ack_count++;
            process_new_opinion();
        }
    }

    public void receive_nack(int proposal_number, Set<Integer> value) {
        if (proposal_number == active_proposal_number) {
            proposed_value.addAll(value);
            nack_count++;
            process_new_opinion();
        }
    }

    void process_new_opinion() {
        if (nack_count > 0 && ack_count + nack_count >= f + 1 && active) {
            active_proposal_number++;
            ack_count = 0;
            nack_count = 0;
            broadcast_value();
        }

        if (ack_count >= f + 1 && active) {
            decide();
            active = false;
        }
    }

    void broadcast_value() {
        String message = "PROPOSAL$" + MessageParser.createSetString(proposed_value) + ';' + active_proposal_number;
        server.bestEffortBroadcast(message);
    }

    void decide() {
        server.logs.add(MessageParser.createSetString(proposed_value));
    }
}
