package de.zalando.hackweek.read_me_the_newz.rss.item;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author dhiller
 */
public class ByteArrayContentProvider extends ContentProvider {

    private final byte[] bytes;

    public ByteArrayContentProvider(Source source, byte[] bytes) {
        super(source);
        this.bytes = bytes;
    }
    
    @Override
    public InputStream provideContent() throws Exception {
        return new ByteArrayInputStream(bytes);
    }
    
}
