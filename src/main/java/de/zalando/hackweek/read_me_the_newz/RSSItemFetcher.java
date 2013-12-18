package de.zalando.hackweek.read_me_the_newz;

import android.os.AsyncTask;
import android.util.Log;
import de.zalando.hackweek.read_me_the_newz.rss.item.HttpURIContentProvider;
import de.zalando.hackweek.read_me_the_newz.rss.item.Item;
import de.zalando.hackweek.read_me_the_newz.rss.item.Source;
import de.zalando.hackweek.read_me_the_newz.rss.item.Type;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
* @author dhiller
*/
class RSSItemFetcher extends AsyncTask<de.zalando.hackweek.read_me_the_newz.RssFeed, Integer, List<Item>> {

    private static final String ID = "RSSItemFetcher";

    @Override
    protected List<Item> doInBackground(final de.zalando.hackweek.read_me_the_newz.RssFeed... params) {
        final List<Item> result = new ArrayList<Item>();
        for (de.zalando.hackweek.read_me_the_newz.RssFeed rssFeed : params) {
            final String u = rssFeed.getUrl();
            Log.d(ID, "doInBackground: Starting fetch of rss items from " + u);

            try {

                final HttpURIContentProvider httpURIContentProvider = new HttpURIContentProvider(
                        new Source(rssFeed.getDescription(), rssFeed.getDescription(), Type.RSS, new URI(u)));
                Log.d(ID, "Read items from " + u);

                final List<Item> items = httpURIContentProvider.extract();
                Log.d(ID, "Got " + items.size() + " items from " + u);
                result.addAll(items);
            } catch (SAXException e) {
                Log.e(ID, "Failed to parse rss items from " + u, e);
            } catch (IOException e) {
                Log.e(ID, "IOException from " + u, e);
            } catch (URISyntaxException e) {
                Log.e(ID, "URISyntaxException from " + u, e);
            } catch (Exception e) {
                Log.e(ID, "URISyntaxException from " + u, e);
            }
        }

        return result;
    }
}
