package cs451.parsing;

import java.util.Set;

public class MyTriple {
    public int round_number;
    public int proposal_number;
    public Set<Integer> value;

    public MyTriple(int round_number, int proposal_number, Set<Integer> value) {
        this.round_number = round_number;
        this.proposal_number = proposal_number;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Round number: " + round_number + ", Proposal number: " + proposal_number + ", value: " + value;
    }
}
