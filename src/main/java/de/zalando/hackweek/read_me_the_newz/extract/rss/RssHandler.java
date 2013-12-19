package de.zalando.hackweek.read_me_the_newz.extract.rss;

import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
* @author dhiller
*/
final class RssHandler extends DefaultHandler {

    private final DateFormat RSS_DATE_PARSER = RssExtractor.newRSSGMTDateFormat();

    final List<RssItem> rssItems = new ArrayList<RssItem>();
    RssItem current;
    StringBuilder builder;

    private Source source;

    public RssHandler(Source source) {
        this.source = source;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             org.xml.sax.Attributes attributes) throws SAXException {
        if (elementName(localName, qName).equals("item")) {
            current = new RssItem();
            current.setType(Type.RSS);
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
            rssItems.add(current);
            current = null;
        }
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
