
package de.zalando.hackweek.read_me_the_newz.extract.feed;

import de.zalando.hackweek.read_me_the_newz.extract.Item;

import java.util.Date;

public class FeedItem extends Item implements Comparable<FeedItem> {

    private String marker;

    private String title;
    private String link;
    private String description;
    private Date from;

    public FeedItem() {
        super();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return "FeedItem{" +
                "type=" + getType() +
                ", marker='" + marker + '\'' +
                ", title='" + title + '\'' +
                ", link='" + link + '\'' +
                ", description='" + description + '\'' +
                ", from=" + from +
                '}';
    }

    @Override
    public int compareTo(FeedItem another) {
        if (this.getFrom() == null)
            return (another.getFrom() == null ? 0 : 1);
        return (another.getFrom() != null ? this.getFrom().compareTo(
                another.getFrom())
                * -1 : -1);
    }

}