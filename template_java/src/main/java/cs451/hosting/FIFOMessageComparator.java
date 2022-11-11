package cs451.hosting;

import java.util.Comparator;

public class FIFOMessageComparator implements Comparator<FIFOMessage> {

    @Override
    public int compare(FIFOMessage fifoMessage, FIFOMessage t1) {
        return Integer.compare(fifoMessage.timestamp, t1.timestamp);
    }
}
