package de.zalando.hackweek.read_me_the_newz.extract.feed;

import java.util.*;

import com.google.common.collect.ImmutableList;

/**
 * An immutable list of feed items.
 * <p/>
 * <p><b>Note: this list is immutable, any attempt to modify this list will result in an
 * {@link UnsupportedOperationException}!</b></p>
 *
 * @author dhiller
 * @see ImmutableList
 */
public class Feed implements Iterable<FeedItem> {

    private final List<FeedItem> items;

    Feed(final List<FeedItem> items) {
        this.items = ImmutableList.copyOf(items);
    }

    public static Feed emptyFeed() {
        return new Feed(Collections.<FeedItem>emptyList());
    }

    public int size() {
        return items.size();
    }

    public Iterator<FeedItem> iterator() {
        return items.iterator();
    }

    public FeedItem get(final int location) {
        return items.get(location);
    }

    public boolean contains(final Object object) {
        return items.contains(object);
    }

}
