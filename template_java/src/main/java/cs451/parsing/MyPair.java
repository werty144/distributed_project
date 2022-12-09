package cs451.parsing;

import java.util.Set;

public class MyPair {
    public int proposal_number;
    public Set<Integer> value;

    public MyPair(int proposal_number, Set<Integer> value) {
        this.proposal_number = proposal_number;
        this.value = value;
    }
}
