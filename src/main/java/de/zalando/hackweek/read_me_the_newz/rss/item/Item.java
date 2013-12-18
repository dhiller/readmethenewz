
package de.zalando.hackweek.read_me_the_newz.rss.item;

import java.util.Date;

public class Item implements Comparable<Item> {

    private Type type;
    private String marker;

    private String title;
    private String link;
    private String description;
    private Date from;

    private boolean isNew;

    public Item() {
        super();
    }

    @Override
    public String toString() {
        return getMarker() + ": " + getTitle() + (isNew() ? " (*)" : "");
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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
    public int compareTo(Item another) {
        if (this.getFrom() == null)
            return (another.getFrom() == null ? 0 : 1);
        return (another.getFrom() != null ? this.getFrom().compareTo(
                another.getFrom())
                * -1 : -1);
    }

    public void setNew() {
        this.isNew = true;
    }

    public boolean isNew() {
        return this.isNew;
    }

}