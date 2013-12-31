package de.zalando.hackweek.read_me_the_newz.extract.feed;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.Charset;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.zalando.hackweek.read_me_the_newz.extract.Source;

public final class FeedExtractor {

    final SAXParserFactory factory = SAXParserFactory.newInstance();
    final FeedHandler feedHandler;
    final SAXParser saxParser;
    final Source source;

    public FeedExtractor(final Source s) throws ParserConfigurationException, SAXException {
        saxParser = factory.newSAXParser();
        this.source = checkNotNull(s);
        this.feedHandler = new FeedHandler(this.source);
    }

    static DateFormat newRSSGMTDateFormat() {
        final SimpleDateFormat gmtDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return gmtDateFormat;
    }

    public Feed extract(final InputStream content) throws FactoryConfigurationError, SAXException, IOException {
        saxParser.parse(new InputSource(new BufferedReader(new InputStreamReader(content, Charset.forName("utf-8")))),
                feedHandler);
        return feedHandler.getFeedItems();
    }

    public Feed extract(final String fullRss) throws SAXException, IOException {
        saxParser.parse(new ByteArrayInputStream(fullRss.getBytes("UTF-8")), feedHandler);
        return feedHandler.getFeedItems();
    }
}
