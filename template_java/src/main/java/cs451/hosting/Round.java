package cs451.hosting;

import java.util.Set;

public class Round {
    int round_number;
    boolean active = false;
    public boolean finished = false;
    int ack_count = 0;
    int nack_count = 0;
    int active_proposal_number = 0;
    Set<Integer> proposed_value = null;

    public Round(int round_number) {
        this.round_number = round_number;
    }
}
