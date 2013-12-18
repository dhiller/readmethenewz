package de.zalando.hackweek.read_me_the_newz;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.widget.ProgressBar;
import org.jsoup.Jsoup;

import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import android.view.View;

import android.widget.TextView;

import nl.matshofman.saxrssreader.RssItem;

public class ReadNewz extends Activity implements TextToSpeech.OnInitListener {

    private static final String ID = "ReadNewz";

    private final ItemPlayback itemPlayback = new ItemPlayback();
    private final ItemPlaybackFeedBackProvider itemPlaybackFeedBackProvider = new ItemPlaybackFeedBackProvider();

    private TextToSpeech textToSpeech;
    private boolean shouldSpeak = true;

    private List<RssFeed> rssFeeds;
    private int rssFeedIndex = 0;

    private ArrayList<RssItem> rssItems;
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
        
        rssFeeds = RssFeed.getFeeds();

        if (savedInstanceState != null) {
            rssFeedIndex = savedInstanceState.getInt("rssFeedIndex");
            rssItemIndex = savedInstanceState.getInt("rssItemIndex");
            rssItemSentenceIndex = savedInstanceState.getInt("rssItemSentenceIndex");
            shouldSpeak = savedInstanceState.getBoolean("shouldSpeak");
        }

        if (textToSpeech == null)
            textToSpeech = new TextToSpeech(this, this);

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
        outState.putInt("rssFeedIndex", rssFeedIndex);
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
        itemPlayback.stopSpeaking();
    }

    // --- TextToSpeech.OnInitListener ---

    /**
     * Called after initialization of TextToSpeech.
     *
     * @param status
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
        rssFeedIndex++;
        if (rssFeedIndex >= rssFeeds.size())
            rssFeedIndex = 0;
        updateRSSItems();
    }

    public void previousFeed(final View v) {
        rssFeedIndex--;
        if (rssFeedIndex < 0)
            rssFeedIndex = rssFeeds.size() - 1;
        updateRSSItems();
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

    private void updateRSSItems() {
        final RssFeed rssFeed = rssFeeds.get(rssFeedIndex);
        final String url = rssFeed.getUrl();

        final Locale language = rssFeed.getLanguage();
        if (setLanguage(language)) {
            setPlaybackCurrentSentence("Language " + language + "not supported!");
            return;
        }

        itemPlayback.stopSpeaking();
        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(rssFeed.getDescription(), TextView.BufferType.EDITABLE);

        setPlaybackCurrentSentence("");

        URL current = null;
        try {
            current = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(ID, "Failed to create URL from " + url, e);
        }

        try {
            rssItems = new RSSItemFetcher().execute(current).get();
        } catch (InterruptedException e) {
            Log.e(ID, "Failed to parse rss items from " + current, e);
        } catch (ExecutionException e) {
            Log.e(ID, "Failed to parse rss items from " + current, e);
        }

        setItemForPlayback();
    }

    private void playbackNextItem() {
        rssItemIndex++;
        rssItemSentenceIndex = 0;
        setItemForPlayback();
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
            final RssItem currentItem = rssItems.get(rssItemIndex);
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

    private void setStatusText(String status, int index, int total) {
        TextView textView = (TextView) findViewById(R.id.status);
        textView.setText(status, TextView.BufferType.EDITABLE);
        ProgressBar bar = (ProgressBar) findViewById(R.id.readProgress);
        bar.setProgress(index);
        bar.setMax(total);
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