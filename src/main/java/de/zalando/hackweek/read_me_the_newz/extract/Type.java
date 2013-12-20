
package de.zalando.hackweek.read_me_the_newz.extract;

import de.zalando.hackweek.read_me_the_newz.extract.feed.FeedExtractor;
import de.zalando.hackweek.read_me_the_newz.extract.feed.FeedItem;

import java.io.InputStream;
import java.util.List;

public enum Type {

    FEED {
        @Override
        public List<FeedItem> extract(ContentProvider p) throws Exception {
            final InputStream content = p.provideContent();
            try {
                return new FeedExtractor(p.source()).extract(content);
            } finally {
                content.close();
            }
        }

    },;

    private Type() {
    }

    public abstract List<FeedItem> extract(ContentProvider p) throws Exception;

}
