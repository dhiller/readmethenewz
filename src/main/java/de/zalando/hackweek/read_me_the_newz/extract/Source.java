package de.zalando.hackweek.read_me_the_newz.extract;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class Source {

    private final Type type;
    private final URI uri;
    private final String longName;
    private final String shortName;

    public Source(String longName, String shortName, Type type, URI source) {
        this.longName = checkNotNull(longName);
        this.shortName = checkNotNull(shortName);
        this.type = checkNotNull(type);
        this.uri = checkNotNull(source);
    }

    public String longName() {
        return longName;
    }

    public String shortName() {
        return shortName;
    }

    public Type type() {
        return type;
    }

    public URI uri() {
        return uri;
    }

}