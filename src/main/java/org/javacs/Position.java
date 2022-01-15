package org.javacs;

public class Position {
    /** 0-based */
    public int line, character;

    public Position() {}

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    @Override
    public String toString() {
        return line + "," + character;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) return false;
        var other = (Position) obj;
        return line == other.line && character == other.character;
    }
    public static final Position NONE = new Position(-1, -1);
}
