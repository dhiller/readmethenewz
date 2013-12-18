package de.zalando.hackweek.read_me_the_newz;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
* @author dhiller
*/
class RssFeed {

    private final String url;
    private final String description;
    private final Locale locale;

    RssFeed(String url) {
        this(url, getTopLevelDomain(url), Locale.ENGLISH);
    }

    RssFeed(String url, String description, Locale locale) {
        this.url = url;
        this.description = description;
        this.locale = locale;
    }

    public static List<RssFeed> getFeeds() {
        return Arrays.asList(
                new RssFeed("http://heise.de.feedsportal.com/c/35207/f/653901/index.rss", "heise.de", Locale.GERMAN),
                new RssFeed("http://rss.slashdot.org/Slashdot/slashdot"),
                new RssFeed("http://feeds.wired.com/wired/index")
        );
    }

    private static String getTopLevelDomain(String url) {
        final int firstPoint = url.indexOf(".");
        return url.substring(firstPoint + 1, url.indexOf("/", firstPoint + 1));
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public Locale getLanguage() {
        return locale;
    }
}
