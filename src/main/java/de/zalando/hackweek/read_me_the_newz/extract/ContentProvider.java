
package de.zalando.hackweek.read_me_the_newz.extract;

import de.zalando.hackweek.read_me_the_newz.extract.rss.RssItem;

import java.io.InputStream;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ContentProvider {

    protected final Source source;

    public ContentProvider(Source source) {
        super();
        this.source = checkNotNull(source);
    }

    public Source source() {
        return source;
    }

    public List<RssItem> extract() throws Exception {
        return source().type().extract(this);
    }

    public abstract InputStream provideContent() throws Exception;

}
