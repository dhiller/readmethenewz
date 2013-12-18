package de.zalando.hackweek.read_me_the_newz;

import android.os.AsyncTask;
import android.util.Log;
import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
* @author dhiller
*/
class RSSItemFetcher extends AsyncTask<URL, Integer, ArrayList<RssItem>> {

    private static final String ID = "RSSItemFetcher";

    @Override
    protected ArrayList<RssItem> doInBackground(final URL... params) {
        final ArrayList<RssItem> result = new ArrayList<RssItem>();
        for (URL u : params) {
            Log.d(ID, "doInBackground: Starting fetch of rss items from " + u);

            try {
                final RssFeed feed = RssReader.read(u);
                Log.d(ID, "Read items from " + u);

                final ArrayList<RssItem> items = feed.getRssItems();
                Log.d(ID, "Got " + items.size() + " items from " + u);
                result.addAll(items);
            } catch (SAXException e) {
                Log.e(ID, "Failed to parse rss items from " + u, e);
            } catch (IOException e) {
                Log.e(ID, "IOException from " + u, e);
            }
        }

        return result;
    }
}
