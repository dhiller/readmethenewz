package de.zalando.hackweek.read_me_the_newz.rss;

import android.util.Log;
import de.zalando.hackweek.read_me_the_newz.rss.item.Item;
import de.zalando.hackweek.read_me_the_newz.rss.item.Source;
import de.zalando.hackweek.read_me_the_newz.rss.item.Type;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Extractor {

    final SAXParserFactory factory = SAXParserFactory.newInstance();
    final Handler handler;
    final SAXParser saxParser;
    final Source source;

    public Extractor(Source s) throws ParserConfigurationException,
            SAXException {
        saxParser = factory.newSAXParser();
        this.source = checkNotNull(s);
        this.handler = new Handler(this.source);
    }

    static DateFormat newRSSGMTDateFormat() {
        final SimpleDateFormat gmtDateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return gmtDateFormat;
    }

    private static final class Handler extends DefaultHandler {

        private final DateFormat RSS_DATE_PARSER = newRSSGMTDateFormat();

        final List<Item> items = new ArrayList<Item>();
        Item current;
        StringBuilder builder;

        private Source source;

        public Handler(Source source) {
            this.source = source;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 org.xml.sax.Attributes attributes) throws SAXException {
            d("Extractor.Handler.startElement", String.format("uri: %s, localName: %s, qName: %s, attributes: %s", 
                    uri, localName, qName, attributes));
            if (elementName(localName, qName).equals("item")) {
                current = new Item();
                current.setType(Type.RSS);
                current.setMarker(source.shortName());
            }
            builder = new StringBuilder();
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            d("Extractor.Handler.characters", String.format("ch: %s, start: %d, length: %d", 
                    ch, start, length));
            builder.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            d("Extractor.Handler.endElement", String.format("uri: %s, localName: %s, qName: %s",
                    uri, localName, qName));
            if (current != null
                    && elementName(localName, qName).equals("title")) {
                current.setTitle(builder.toString());
            }
            if (current != null
                    && elementName(localName, qName).equals("description")) {
                current.setDescription(builder.toString());
            }
            if (current != null && elementName(localName, qName).equals("link")) {
                current.setLink(builder.toString());
            }
            if (current != null
                    && elementName(localName, qName).equals("pubDate")) {
                final String dateAsString = builder.toString();
                try {
                    current.setFrom(RSS_DATE_PARSER.parse(dateAsString));
                } catch (ParseException e) {
                    System.out.println(RSS_DATE_PARSER.format(new Date())); //$NON-NLS-1$ // TODO: Remove
                    e.printStackTrace(); // TODO
                }
            }
            if (elementName(localName, qName).equals("item")) {
                items.add(current);
                current = null;
            }
        }

        private void d(String identifier, String message) {
            Log.d(identifier, message);
        }

        /**
         * Within a Junit Test the runtime behaves seemingly different, so we
         * have to use the name value that's set?! TODO: find out why
         *
         * @param localName the local name
         * @param qName the qName
         * @return the element name
         */
        private String elementName(String localName, String qName) {
            return localName != null && localName.length() > 0 ? localName
                    : qName;
        }

    }

    public List<Item> extract(InputStream content)
            throws FactoryConfigurationError, ParserConfigurationException,
            SAXException, IOException {
        saxParser.parse(new InputSource(new BufferedReader(
                new InputStreamReader(content, Charset.forName("utf-8")))),
                handler);
        return handler.items;
    }

    public List<Item> extract(String fullRss) throws SAXException, IOException {
        saxParser.parse(new ByteArrayInputStream(fullRss.getBytes("UTF-8")),
                handler);
        return handler.items;
    }
}