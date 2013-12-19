
package de.zalando.hackweek.read_me_the_newz.extract;

import de.zalando.hackweek.read_me_the_newz.extract.rss.RssExtractor;
import de.zalando.hackweek.read_me_the_newz.extract.rss.RssItem;

import java.io.InputStream;
import java.util.List;

public enum Type {

    RSS {
        @Override
        public List<RssItem> extract(ContentProvider p) throws Exception {
            final InputStream content = p.provideContent();
            try {
                return new RssExtractor(p.source()).extract(content);
            } finally {
                content.close();
            }
        }

    },;

    private Type() {
    }

    public abstract List<RssItem> extract(ContentProvider p) throws Exception;

}
