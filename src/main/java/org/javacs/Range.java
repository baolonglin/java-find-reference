package org.javacs;

public class Range {
    public Position start, end;

    public Range() {}

    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Range)) return false;

        var other = (Range)obj;
        return start.equals(other.start) && end.equals(other.end);
    }
    public static final Range NONE = new Range(Position.NONE, Position.NONE);
}
