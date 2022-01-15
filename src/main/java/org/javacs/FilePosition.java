package org.javacs;

import java.nio.file.Path;

public class FilePosition {
    public Path path;
    public int line, character; // start with 1

    public FilePosition(Path p, int l, int c) {
        path = p;
        line = l;
        character = c;
    }

    @Override
    public int hashCode() {
        return (path.toString() + ":" + line + ":" + character).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FilePosition)) return false;
        var other = (FilePosition)obj;
        return path.equals(other.path) && line == other.line && character == other.character;
    }
}
