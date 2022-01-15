package org.javacs;

import java.net.URI;

public class Location {
    public URI uri;
    public Range range;

    public Location() {}

    public Location(URI uri, Range range) {
        this.uri = uri;
        this.range = range;
    }

    @Override
    public String toString() {
        return uri.toString() + ":" + range.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Location)) return false;
        var other = (Location)obj;
        return uri.equals(other.uri) && range.equals(other.range);
    }

    @Override
    public int hashCode() {
        return (uri.toString() + ":" + range.toString()).hashCode();
    }

    public static final Location NONE = new Location(null, Range.NONE);
}
