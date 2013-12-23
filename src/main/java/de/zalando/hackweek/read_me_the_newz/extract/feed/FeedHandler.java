package de.zalando.hackweek.read_me_the_newz.extract.feed;

import static de.zalando.hackweek.read_me_the_newz.extract.feed.FeedElement.*;

import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhiller
 */
final class FeedHandler extends DefaultHandler {

    private final List<FeedItem> feedItems = new ArrayList<FeedItem>();
    private FeedItem current;
    private StringBuilder builder;

    private Source source;

    public FeedHandler(Source source) {
        this.source = source;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             org.xml.sax.Attributes attributes) throws SAXException {
        if (ITEM.isElementNameContained(localName, qName)) {
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
        if (current != null) {
            FeedElement.elementFor(localName, qName).setProperty(current, builder.toString());
        }
        if (ITEM.isElementNameContained(localName, qName)) {
            getFeedItems().add(current);
            current = null;
        }
    }

    public List<FeedItem> getFeedItems() {
        return feedItems;
    }
}
