package de.zalando.hackweek.read_me_the_newz;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.common.base.Throwables;
import de.zalando.hackweek.read_me_the_newz.rss.item.ByteArrayContentProvider;
import de.zalando.hackweek.read_me_the_newz.rss.item.Item;
import de.zalando.hackweek.read_me_the_newz.rss.item.Source;
import de.zalando.hackweek.read_me_the_newz.rss.item.Type;
import org.jsoup.Jsoup;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

public class ReadNewz extends Activity implements TextToSpeech.OnInitListener {

    private static final String ID = "ReadNewz";

    private final ItemPlayback itemPlayback = new ItemPlayback();
    private final ItemPlaybackFeedBackProvider itemPlaybackFeedBackProvider = new ItemPlaybackFeedBackProvider();

    private TextToSpeech textToSpeech;
    private boolean shouldSpeak = true;

    private AsyncTask<?, ?, ?> activeItemFetcher;

    private List<RssFeedDescriptor> rssFeedDescriptors;
    private int rssFeedDescriptorIndex = 0;

    private List<Item> rssItems;
    private int rssItemIndex = 0;
    private int rssItemSentenceIndex = 0;

    // --- Application lifecycle callbacks ---

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(ID, "onCreate");
        setContentView(R.layout.main);

        rssFeedDescriptors = RssFeedDescriptor.getFeeds();

        if (savedInstanceState != null) {
            rssFeedDescriptorIndex = savedInstanceState.getInt("rssFeedIndex");
            rssItemIndex = savedInstanceState.getInt("rssItemIndex");
            rssItemSentenceIndex = savedInstanceState.getInt("rssItemSentenceIndex");
            shouldSpeak = savedInstanceState.getBoolean("shouldSpeak");
        }

        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(ID, "onResume");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(ID, "onSaveInstanceState");
        outState.putInt("rssFeedIndex", rssFeedDescriptorIndex);
        outState.putInt("rssItemIndex", rssItemIndex);
        outState.putInt("rssItemSentenceIndex", rssItemSentenceIndex);
        outState.putBoolean("shouldSpeak", shouldSpeak);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(ID, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(ID, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(ID, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(ID, "onDestroy");
        if (activeItemFetcher != null) {
            activeItemFetcher.cancel(true);
        }
        itemPlayback.stopSpeaking();
    }

    // --- TextToSpeech.OnInitListener ---

    /**
     * Called after initialization of TextToSpeech.
     *
     * @param status the initialization result
     */
    @Override
    public void onInit(final int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(ID, "TextToSpeech Init failed!");
            return;
        }

        findViewById(R.id.previousFeed).setEnabled(true);
        findViewById(R.id.nextFeed).setEnabled(true);

        itemPlayback.setTextToSpeech(textToSpeech);
        itemPlayback.setSentenceIndex(rssItemSentenceIndex);
        itemPlayback.setItemPlaybackListener(itemPlaybackFeedBackProvider);

        @SuppressWarnings("deprecation") // UtteranceProgressListener is API level 15
        final int listenerSetResult = textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(final String utteranceId) {
                itemPlayback.continueWithNextSentence();
            }
        });
        Log.d(ID, "Result for setListener: " + listenerSetResult);

        updateRSSItems();
    }

    public boolean setLanguage(Locale language) {
        int result = textToSpeech.setLanguage(language);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(ID, "Language ENGLISH not supported!");
            return true;
        }
        return false;
    }

    // --- UI callbacks ---

    public void nextFeed(final View v) {
        startPlaybackForNextFeed();
    }

    public void previousFeed(final View v) {
        startPlaybackForPreviousFeed();
    }

    public void previous(final View v) {
        playbackPreviousItem();
    }

    public void next(final View v) {
        playbackNextItem();
    }

    public void playPause(final View v) {
        shouldSpeak = !shouldSpeak;
        itemPlayback.toggleSpeaking();
    }

    // --- Others ---

    private void startPlaybackForNextFeed() {
        rssFeedDescriptorIndex++;
        if (rssFeedDescriptorIndex >= rssFeedDescriptors.size())
            rssFeedDescriptorIndex = 0;
        updateRSSItems();
    }

    private void startPlaybackForPreviousFeed() {
        rssFeedDescriptorIndex--;
        if (rssFeedDescriptorIndex < 0)
            rssFeedDescriptorIndex = rssFeedDescriptors.size() - 1;
        updateRSSItems();
    }

    private void updateRSSItems() {
        final RssFeedDescriptor rssFeedDescriptor = rssFeedDescriptors.get(rssFeedDescriptorIndex);

        final Locale language = rssFeedDescriptor.getLanguage();
        if (setLanguage(language)) {
            setPlaybackCurrentSentence("Language " + language + "not supported!");
            return;
        }

        itemPlayback.stopSpeaking();

        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(rssFeedDescriptor.getDescription(), TextView.BufferType.EDITABLE);

        setPlaybackCurrentSentence("");

        if (activeItemFetcher != null) {
            activeItemFetcher.cancel(true);
        }

        activeItemFetcher = new RssFeedDownloadTask().execute(rssFeedDescriptor);
    }

    private void playbackNextItem() {
        if(rssItemIndex >= rssItems.size()) {
            startPlaybackForNextFeed();
        }
        else{
            rssItemIndex++;
            rssItemSentenceIndex = 0;
            setItemForPlayback();
        }
    }

    private void playbackPreviousItem() {
        rssItemIndex--;
        rssItemSentenceIndex = 0;
        setItemForPlayback();
    }

    private void setItemForPlayback() {

        findViewById(R.id.previous).setEnabled(rssItemIndex > 0);
        findViewById(R.id.next).setEnabled(rssItemIndex < rssItems.size());
        findViewById(R.id.playPause).setEnabled(rssItems.size() > 0);

        String title = "";
        String text = "";
        final boolean hasItem = rssItems != null && rssItemIndex >= 0 && rssItems.size() > rssItemIndex;
        if (hasItem) {
            final Item currentItem = rssItems.get(rssItemIndex);
            title = Jsoup.parse(currentItem.getTitle()).text();
            text = Jsoup.parse(currentItem.getDescription()).text();
            itemPlayback.setItemForPlayback(currentItem);
            itemPlayback.setSentenceIndex(rssItemSentenceIndex);
            if (shouldSpeak)
                itemPlayback.startSpeaking();
        }

        setPlaybackCurrentSentence(text);
        setTitle(title);
        setStatusText(shouldSpeak ? "Reading" : "Paused", rssItemSentenceIndex, itemPlayback.numberOfSentences());
        setPlaybackCurrentSentence(itemPlayback.getCurrentSentence());
    }

    private void setTitle(String titleText) {
        TextView textView = (TextView) findViewById(R.id.rssItemTitle);
        textView.setText(titleText, TextView.BufferType.EDITABLE);
    }

    private void setPlaybackCurrentSentence(String text) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text, TextView.BufferType.EDITABLE);
    }

    private void setStatusTextIndeterminate(String status) {
        setStatusText(status, 0, 0);
    }

    private void setStatusText(String status, int index, int total) {
        TextView textView = (TextView) findViewById(R.id.status);
        textView.setText(status, TextView.BufferType.EDITABLE);
        ProgressBar bar = (ProgressBar) findViewById(R.id.readProgress);
        if (index == 0 && total == 0) {
            bar.setIndeterminate(true);
            bar.setProgress(0);
            bar.setMax(1);
        }else {
            bar.setIndeterminate(false);
            bar.setProgress(index);
            bar.setMax(total);
        }
    }

    private final class RssFeedDownloadTask extends DownloadTask<RssFeedDescriptor> {
        RssFeedDownloadTask() {
            super(ReadNewz.this);
        }

        @Override
        protected void onProgressUpdate(final Progress... values) {
            final Progress progress = values[0];

            if (progress.isTotalBytesAvailable()) {
                Log.i(ID, progress.getBytesReceived() + "/"+ progress.getTotalBytes());
                setStatusText(format("Downloaded %s%%…", progress.getPercentage()),
                        (int) progress.getBytesReceived(), (int) progress.getTotalBytes());
            } else {
                Log.i(ID, Long.toString(progress.getBytesReceived()));
                setStatusTextIndeterminate(format("Downloaded %d bytes…", progress.getBytesReceived()));
            }
        }

        @Override
        protected void onPostExecute(final Result<RssFeedDescriptor> result) {
            if (activeItemFetcher != this) {
                return;
            }

            activeItemFetcher = null;

            if (isCancelled()) {
                return;
            }

            if (!result.isSuccess()) {
                Log.e(ID, "RSS download failed", result.getException());
                return;
            }

            setStatusTextIndeterminate("Processing RSS feed…");

            final RssFeedDescriptor descriptor = result.getItem();

            activeItemFetcher = new AsyncTask<Void, Void, List<Item>>() {
                @Override
                protected List<Item> doInBackground(final Void... unused) {
                    try {
                        final Source source = new Source(descriptor.getDescription(), descriptor.getDescription(), Type.RSS, new URI(descriptor.getUrl()));
                        return new ByteArrayContentProvider(source,result.getContent()).extract();
                    } catch (Exception e) {
                        Log.e(ID, "RSS parsing failed", result.getException());
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(final List<Item> result) {
                    if (activeItemFetcher != this) {
                        return;
                    }

                    activeItemFetcher = null;

                    if (isCancelled()) {
                        return;
                    }

                    setStatusTextIndeterminate("");

                    rssItems = result;
                    rssItemIndex = 0;
                    setItemForPlayback();
                }

                @Override
                protected void onCancelled() {
                    if (activeItemFetcher == this) {
                        activeItemFetcher = null;
                    }
                }
            }.execute();

        }

        @Override
        protected void onCancelled(final Result result) {
            if (activeItemFetcher == this) {
                activeItemFetcher = null;
            }
        }

        @Override
        protected URL getUrl(RssFeedDescriptor url) {
            try {
                return new URL(url.getUrl());
            } catch (MalformedURLException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private class ItemPlaybackFeedBackProvider extends ItemPlaybackListener {

        @Override
        void beganWith(int index, int total, final String sentence) {
            rssItemSentenceIndex = index;
            setStatusText("Reading", index, total);
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run() {
                    setPlaybackCurrentSentence(sentence);
                }
            });
        }

        @Override
        void stoppedAt(int index, int total, String sentence) {
            setStatusText("Paused", index, total);
        }

        @Override
        void finishedItem(int index, int total, String sentence) {
            setStatusText("Finished", index, total);
        }

        @Override
        void finishedAll(int total) {
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run() {
                    playbackNextItem();
                }
            });
        }

        private void setStatusText(final String status, final int index, final int total) {
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                @Override
                public void run() {
                    ReadNewz.this.setStatusText(status, index, total);
                }
            });
        }

    }
}
