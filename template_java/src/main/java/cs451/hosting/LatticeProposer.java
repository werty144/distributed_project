package cs451.hosting;

import cs451.Message;
import cs451.parsing.MessageParser;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.*;

public class LatticeProposer {
    int roundCounter = 0;
    static int MAX_ROUNDS = 10;
    final Set<Round> rounds = Collections.synchronizedSet(new HashSet<>());
    Server server;
    int f;

    public LatticeProposer(Server server) {
        this.server = server;
        f = (server.hosts.size() - 1) / 2;
    }

    public boolean propose(Set<Integer> proposal) {
        Round round;
        synchronized (rounds) {
            if (rounds.size() >= MAX_ROUNDS) return false;
            round = new Round(roundCounter++);
            rounds.add(round);
        }
        round.proposed_value = proposal;
        round.active = true;
        round.active_proposal_number++;
        round.ack_count = 0;
        round.nack_count = 0;
        broadcast_value(round);
        return true;
    }

    public void receive_ack(int round_number, int proposal_number) {
        Optional<Round> optRound;
        synchronized (rounds) {
            optRound = rounds.stream().filter(it -> it.round_number == round_number).findAny();
        }
        if (optRound.isEmpty()) return;
        Round round = optRound.get();
        if (proposal_number == round.active_proposal_number) {
            round.ack_count++;
            process_new_opinion(round);
        }
    }

    public void receive_nack( int round_number, int proposal_number, Set<Integer> value) {
        Optional<Round> optRound;
        synchronized (rounds) {
            optRound = rounds.stream().filter(it -> it.round_number == round_number).findAny();
        }
        if (optRound.isEmpty()) return;
        Round round = optRound.get();
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
        synchronized (rounds) {
            rounds.remove(round);
        }
        server.bestEffortBroadcast(MessageParser.createDecided(round.round_number));
    }
}
