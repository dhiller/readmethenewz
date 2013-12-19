package de.zalando.hackweek.read_me_the_newz.extract.rss;

import com.google.common.base.Throwables;
import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author dhiller
 */
public class RssFeedSource extends Source {

    private final Locale locale;

    private RssFeedSource(URI uri) {
        this(uri, getTopLevelDomain(uri), Locale.ENGLISH);
    }

    private RssFeedSource(URI uri, String description, Locale locale) {
        super(description, description, Type.RSS, uri);
        this.locale = locale;
    }

    public static List<RssFeedSource> getFeeds() {
        try {
            return Arrays.asList(
                    new RssFeedSource(new URI("http://heise.de.feedsportal.com/c/35207/f/653901/index.rss"), "heise.de", Locale.GERMAN),
                    new RssFeedSource(new URI("http://rss.slashdot.org/Slashdot/slashdot")),
                    new RssFeedSource(new URI("http://feeds.wired.com/wired/index"))
            );
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String getTopLevelDomain(URI uri) {
        return uri.getHost().substring(uri.getHost().indexOf(".") + 1);
    }

    public String getUrl() {
        return uri().toString();
    }

    public Locale getLanguage() {
        return locale;
    }
}
