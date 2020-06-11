package de.julielab.gepi.core.retrieval.data;

import java.util.ArrayList;
import java.util.List;

public class AggregatedEventsRetrievalResult {
    private List<String> arg1Names = new ArrayList<>();
    private List<String> arg2Names = new ArrayList<>();
    private List<String> arg1Ids = new ArrayList<>();
    private List<String> arg2Ids = new ArrayList<>();
    private List<Integer> counts = new ArrayList<>();

    private int pos = -1;

    public void add(String arg1Name, String arg2Name, String arg1Id, String arg2Id, int count) {
        arg1Names.add(arg1Name);
        arg2Names.add(arg2Name);
        arg1Ids.add(arg1Id);
        arg2Ids.add(arg2Id);
        counts.add(count);
    }

    public int size() {
        return arg1Names.size();
    }

    public String getArg1Name() {
        return arg1Names.get(pos);
    }

    public String getArg2Name() {
        return arg2Names.get(pos);
    }

    public String getArg1Id() {
        return arg1Ids.get(pos);
    }

    public String getArg2Id() {
        return arg2Ids.get(pos);
    }

    public int getCount() {
        return counts.get(pos);
    }

    public boolean increment() {
        ++pos;
        return pos < arg1Names.size();
    }

    /**
     * Sets the position to -1 and thus makes it ready for another iteration using {@link #increment()};
     *
     */
    public void rewind() {
        pos = -1;
    }

    public void seek(int i) {
        if (i < 0)
            throw new IllegalArgumentException("Position is supposed to be set to " + i + " but must be >= 0.");
        if (i < size())
            pos = i;
        else
            throw new IllegalArgumentException("Position is supposed to be set to " + i + " but the size is " + size());
    }
}
