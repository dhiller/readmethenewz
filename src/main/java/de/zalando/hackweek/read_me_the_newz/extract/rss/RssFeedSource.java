package de.zalando.hackweek.read_me_the_newz.extract.rss;

import android.content.res.XmlResourceParser;
import com.google.common.base.Throwables;
import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dhiller
 */
public class RssFeedSource extends Source {

    private final Locale locale;

    private RssFeedSource(URI uri, String description, Locale locale) {
        super(description, description, Type.RSS, uri);
        this.locale = locale;
    }

    public static List<RssFeedSource> getFeeds(XmlResourceParser xml) {
        final List<RssFeedSource> result = new ArrayList<RssFeedSource>();
        try {
            int next;
            while((next = xml.next()) != XmlPullParser.END_DOCUMENT) {
                if(next != XmlPullParser.START_TAG)
                    continue;
                final String type = xml.getAttributeValue(null, "type");
                if(!"rss".equals(type))
                    continue;
                final URI xmlUrl = new URI(xml.getAttributeValue(null, "xmlUrl"));
                final String htmlUrl = xml.getAttributeValue(null, "htmlUrl");
                final String longDescription = xml.getAttributeValue(null, "text");
                result.add(new RssFeedSource(xmlUrl,longDescription, getLocale(htmlUrl)));
            }
        } catch (XmlPullParserException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        return result;
    }

    private static Locale getLocale(String htmlUrl) {
        if(htmlUrl==null)
            return Locale.ENGLISH;
        final String tld = getTLD(htmlUrl);
        if("de".equals(tld))
            return Locale.GERMAN;
        return Locale.ENGLISH;
    }

    private static String getHostName(URI uri) {
        return uri.getHost().substring(uri.getHost().indexOf(".") + 1);
    }

    private static String getTLD(String url) {
        final Matcher matcher = Pattern.compile(".*\\.([a-z]+)/?.*").matcher(url);
        if(!matcher.find())
            return "???";
        return matcher.group(1);
    }

    public String getUrl() {
        return uri().toString();
    }

    public Locale getLanguage() {
        return locale;
    }
}
