
package de.zalando.hackweek.read_me_the_newz.extract;

import java.io.InputStream;

import de.zalando.hackweek.read_me_the_newz.extract.feed.Feed;
import de.zalando.hackweek.read_me_the_newz.extract.feed.FeedExtractor;

public enum Type {

    FEED {
        @Override
        public Feed extract(final ContentProvider p) throws Exception {
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

    public abstract Feed extract(ContentProvider p) throws Exception;

}
