package de.zalando.hackweek.read_me_the_newz.extract.feed;

import de.zalando.hackweek.read_me_the_newz.extract.Source;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FeedExtractor {

    final SAXParserFactory factory = SAXParserFactory.newInstance();
    final FeedHandler feedHandler;
    final SAXParser saxParser;
    final Source source;

    public FeedExtractor(Source s) throws ParserConfigurationException,
            SAXException {
        saxParser = factory.newSAXParser();
        this.source = checkNotNull(s);
        this.feedHandler = new FeedHandler(this.source);
    }

    static DateFormat newRSSGMTDateFormat() {
        final SimpleDateFormat gmtDateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return gmtDateFormat;
    }

    public List<FeedItem> extract(InputStream content)
            throws FactoryConfigurationError, SAXException, IOException {
        saxParser.parse(new InputSource(new BufferedReader(
                new InputStreamReader(content, Charset.forName("utf-8")))),
                feedHandler);
        return feedHandler.feedItems;
    }

    public List<FeedItem> extract(String fullRss) throws SAXException, IOException {
        saxParser.parse(new ByteArrayInputStream(fullRss.getBytes("UTF-8")),
                feedHandler);
        return feedHandler.feedItems;
    }
}