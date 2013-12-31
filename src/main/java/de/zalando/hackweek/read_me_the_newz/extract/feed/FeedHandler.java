package de.zalando.hackweek.read_me_the_newz.extract.feed;

import static de.zalando.hackweek.read_me_the_newz.extract.feed.FeedElement.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;

/**
 * @author dhiller
 */
final class FeedHandler extends DefaultHandler {

    private final List<FeedItem> feedItems = new ArrayList<FeedItem>();
    private FeedItem current;
    private StringBuilder builder;

    private Source source;

    public FeedHandler(final Source source) {
        this.source = source;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName,
                             final org.xml.sax.Attributes attributes) throws SAXException {
        if (ITEM.isElementNameContained(localName, qName)) {
            current = new FeedItem();
            current.setType(Type.FEED);
            current.setMarker(source.shortName());
        }

        builder = new StringBuilder();
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (current != null) {
            FeedElement.elementFor(localName, qName).setProperty(current, builder.toString());
        }

        if (ITEM.isElementNameContained(localName, qName)) {
            feedItems.add(current);
            current = null;
        }
    }

    public Feed feedWithOldestItemsFirst() {
        final List<FeedItem> result = new ArrayList<FeedItem>();
        result.addAll(feedItems);
        Collections.sort(result, new Comparator<FeedItem>() {
            @Override
            public int compare(FeedItem lhs, FeedItem rhs) {
                return (lhs.getFrom().before(rhs.getFrom()) ? -1 : (lhs.getFrom().after(rhs.getFrom()) ? 1 : 0));
            }
        });
        return new Feed(result);
    }
}
