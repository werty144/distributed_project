package cs451.hosting;

import cs451.Message;
import cs451.parsing.MessageParser;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.*;

class AcceptorRound {
    static int lastFreeIndex = 0;
    static Map<Integer, Integer> value2index = new HashMap<>();
    static Map<Integer, Integer> index2value = new HashMap<>();
    Integer round_number;
    BitSet accepted_value;
    int nDecided;

    public AcceptorRound(int round_number, int nValues) {
        this.round_number = round_number;
        accepted_value = new BitSet(nValues);
        nDecided = 0;
    }

    public void setAccepted_value(Set<Integer> accepted_set) {
        accepted_value.clear();
        for (int x : accepted_set) {
            if (!value2index.containsKey(x)) {
                value2index.put(x, lastFreeIndex);
                index2value.put(lastFreeIndex, x);
                lastFreeIndex++;
            }
            accepted_value.set(value2index.get(x));
        }
    }

    public Set<Integer> getAcceptedValue() {
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < accepted_value.size(); i++) {
            if (accepted_value.get(i)) {
                ret.add(index2value.get(i));
            }
        }
        return ret;
    }

    @Override
    public int hashCode() {
        return round_number.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AcceptorRound)) return false;
        return Objects.equals(((AcceptorRound) obj).round_number, round_number);
    }
}

public class LatticeAcceptor {
    Server server;
    Set<AcceptorRound> rounds = new HashSet<>();
    int nValues;

    public LatticeAcceptor(Server server) {
        this.server = server;
    }

    public void setnValues(int nValues) {
        this.nValues = nValues;
    }

    public void receive_proposal(int round_number, Set<Integer> proposed_value, Integer proposal_number, Host sender) {
        Optional<AcceptorRound> optRound = rounds.stream().filter(it -> it.round_number == round_number).findAny();
        AcceptorRound round;
        if (optRound.isEmpty()) {
            round = new AcceptorRound(round_number, nValues);
            rounds.add(round);
        } else {
            round = optRound.get();
        }

        if (proposed_value.containsAll(round.getAcceptedValue())) {
            round.setAccepted_value(proposed_value);
            sendAck(round, proposal_number, sender);
        } else {
            Set<Integer> curValue = round.getAcceptedValue();
            curValue.addAll(proposed_value);
            round.setAccepted_value(curValue);
            sendNack(round, proposal_number, sender);
        }
    }

    void sendAck(AcceptorRound round, Integer proposal_number, Host toWhom) {
        Message message = MessageParser.createLatticeAck(round.round_number, proposal_number);
        server.sendMessageSL(message, toWhom);
    }

    void sendNack(AcceptorRound round, Integer proposal_number, Host toWhom) {
        Message message = MessageParser.createLatticeNack(
                round
                        .round_number,
                proposal_number,
                round.getAcceptedValue()
        );
        server.sendMessageSL(message, toWhom);
    }

    void hostDecided(int round_number, int hostID) {
        Optional<AcceptorRound> optRound = rounds.stream().filter(it -> it.round_number == round_number).findAny();
        AcceptorRound round;
        if (optRound.isEmpty()) {
            round = new AcceptorRound(round_number, nValues);
            rounds.add(round);
        } else {
            round = optRound.get();
        }
        round.nDecided++;
        if (round.nDecided == server.hosts.size()) {
            rounds.remove(round);
        }
    }
}
