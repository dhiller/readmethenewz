
package de.zalando.hackweek.read_me_the_newz.rss.item;

import de.zalando.hackweek.read_me_the_newz.rss.Extractor;

import java.io.InputStream;
import java.util.List;

public enum Type {

    RSS {
        @Override
        public List<Item> extract(ContentProvider p) throws Exception {
            final InputStream content = p.provideContent();
            try {
                return new Extractor(p.source()).extract(content);
            } finally {
                content.close();
            }
        }

        @Override
        public ContentProvider createProvider(Source s) {
            return new HttpURIContentProvider(s);
        }

    },;

    private Type() {
    }

    public abstract List<Item> extract(ContentProvider p) throws Exception;

    public abstract ContentProvider createProvider(Source s);

}
