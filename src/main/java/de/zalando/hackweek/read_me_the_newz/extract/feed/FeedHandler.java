package de.zalando.hackweek.read_me_the_newz.extract.feed;

import com.google.common.base.Throwables;
import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author dhiller
 */
final class FeedHandler extends DefaultHandler {

    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    final List<FeedItem> feedItems = new ArrayList<FeedItem>();
    FeedItem current;
    StringBuilder builder;

    private Source source;

    public FeedHandler(Source source) {
        this.source = source;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             org.xml.sax.Attributes attributes) throws SAXException {
        if (isItem(localName, qName)) {
            current = new FeedItem();
            current.setType(Type.FEED);
            current.setMarker(source.shortName());
        }
        builder = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (current != null
                && isTitle(localName, qName)) {
            current.setTitle(builder.toString());
        }
        if (current != null
                && isDescription(localName, qName)) {
            current.setDescription(builder.toString());
        }
        if (current != null && isLink(localName, qName)) {
            current.setLink(builder.toString());
        }
        if (current != null
                && isPubDate(localName, qName)) {
            final String dateAsString = builder.toString();
            try {
                current.setFrom(FeedExtractor.newRSSGMTDateFormat().parse(dateAsString));
            } catch (ParseException e) {
                try {
                    current.setFrom(new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(dateAsString));
                } catch (ParseException e2) {
                    throw Throwables.propagate(e2);
                }
            }
        }
        if (isItem(localName, qName)) {
            feedItems.add(current);
            current = null;
        }
    }

    private boolean isItem(String localName, String qName) {
        return isElementNameContained(localName, qName, "item", "entry");
    }

    private boolean isPubDate(String localName, String qName) {
        return isElementNameContained(localName, qName, "pubDate", "updated");
    }

    private boolean isLink(String localName, String qName) {
        return isElementNameContained(localName, qName, "link");
    }

    private boolean isDescription(String localName, String qName) {
        return isElementNameContained(localName, qName, "description", "summary", "content");
    }

    private boolean isTitle(String localName, String qName) {
        return isElementNameContained(localName, qName, "title");
    }

    private boolean isElementNameContained(String localName, String qName, String... elementNames) {
        return Arrays.asList(elementNames).contains(elementName(localName, qName));
    }

    /**
     * Within a Junit Test the runtime behaves seemingly different, so we
     * have to use the name value that's set?! TODO: find out why
     *
     * @param localName the local name
     * @param qName     the qName
     * @return the element name
     */
    private String elementName(String localName, String qName) {
        return localName != null && localName.length() > 0 ? localName
                : qName;
    }

}
