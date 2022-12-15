package cs451.hosting;

import cs451.Message;
import cs451.parsing.MessageParser;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LatticeProposer {
    List<Round> rounds = new ArrayList<>();
    Server server;
    int f;

    public LatticeProposer(Server server) {
        this.server = server;
        f = (server.hosts.size() - 1) / 2;
    }

    public Round propose(Set<Integer> proposal) {
        Round round = new Round(rounds.size());
        rounds.add(round);
        round.proposed_value = proposal;
        round.active = true;
        round.active_proposal_number++;
        round.ack_count = 0;
        round.nack_count = 0;
        broadcast_value(round);
        return round;
    }

    public void receive_ack(int round_number, int proposal_number) {
        Round round = rounds.get(round_number);
        if (proposal_number == round.active_proposal_number) {
            round.ack_count++;
            process_new_opinion(round);
        }
    }

    public void receive_nack( int round_number, int proposal_number, Set<Integer> value) {
        Round round = rounds.get(round_number);
        if (proposal_number == round.active_proposal_number) {
            round.proposed_value.addAll(value);
            round.nack_count++;
            process_new_opinion(round);
        }
    }

    void process_new_opinion(Round round) {
        if (round.nack_count > 0 && round.ack_count + round.nack_count >= f + 1 && round.active) {
            round.active_proposal_number++;
            round.ack_count = 0;
            round.nack_count = 0;
            broadcast_value(round);
        }

        if (round.ack_count >= f + 1 && round.active) {
            decide(round);
            round.active = false;
        }
    }

    void broadcast_value(Round round) {
        Message message = MessageParser.createLatticeProposal(
                round.round_number,
                round.active_proposal_number,
                round.proposed_value);
        server.bestEffortBroadcast(message);
    }

    void decide(Round round) {
        server.decide(round.round_number, round.proposed_value);
        round.finished = true;
    }
}
