package de.zalando.hackweek.read_me_the_newz.extract.feed;

import com.google.common.base.Throwables;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author dhiller
 * @since 23.12.13
 */
enum FeedElement {
    ID("guid", "id") {
        @Override
        void setProperty(FeedItem current, String value) {
            current.setId(value);
        }
    },
    ITEM("item", "entry"),
    PUB_DATE("pubDate", "updated") {
        @Override
        void setProperty(FeedItem current, String value) {
            current.setFrom(convertToDate(value));
        }
    },
    LINK("link") {
        @Override
        void setProperty(FeedItem current, String value) {
            current.setLink(value);
        }
    },
    DESCRIPTION("description", "summary", "content") {
        @Override
        void setProperty(FeedItem current, String value) {
            current.setDescription(value);
        }
    },
    TITLE("title") {
        @Override
        void setProperty(FeedItem current, String value) {
            current.setTitle(value);
        }
    },
    OTHER(),;

    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
    private final List<String> names;

    private FeedElement(String... names) {
        this.names = Arrays.asList(names);
    }

    static FeedElement elementFor(String localName, String qName) {
        for (FeedElement e : values()) {
            if (e.isElementNameContained(localName, qName)) {
                return e;
            }
        }
        return OTHER;
    }

    void setProperty(FeedItem current, String value) {
    }

    boolean isElementNameContained(String localName, String qName) {
        return names.contains(elementName(localName, qName));
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

    private static Date convertToDate(String dateAsString) {
        Date from;
        try {
            from = FeedExtractor.newRSSGMTDateFormat().parse(dateAsString);
        } catch (ParseException e) {
            try {
                from = new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(dateAsString);
            } catch (ParseException e2) {
                try {
                    from = new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(dateAsString.replaceAll("([+-][0-9]{2}:[0-9]{2})$", "GMT$1"));
                } catch (ParseException e3) {
                    throw Throwables.propagate(e3);
                }
            }
        }
        return from;
    }

}
