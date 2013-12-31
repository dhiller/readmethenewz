
package de.zalando.hackweek.read_me_the_newz.extract;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;

import de.zalando.hackweek.read_me_the_newz.extract.feed.Feed;

public abstract class ContentProvider {

    protected final Source source;

    public ContentProvider(final Source source) {
        super();
        this.source = checkNotNull(source);
    }

    public Source source() {
        return source;
    }

    public Feed extract() throws Exception {
        return source().type().extract(this);
    }

    public abstract InputStream provideContent() throws Exception;

}
