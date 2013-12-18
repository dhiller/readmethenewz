package de.zalando.hackweek.read_me_the_newz;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
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

    private static final String[] urls = new String[]{
            "http://rss.slashdot.org/Slashdot/slashdot",
//            "http://www.google.com/alerts/feeds/10782259317798652848/4797091171555319245", // Zalando news feed
            "http://feeds.wired.com/wired/index",
    };
    private static final String ID = "ReadNewz";

    private final ItemPlayback itemPlayback = new ItemPlayback();

    private TextToSpeech textToSpeech;

    private ArrayList<RssItem> rssItems;
    private int rssFeedIndex = 0;
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
        
        if(savedInstanceState!=null) {
            rssFeedIndex = savedInstanceState.getInt("rssFeedIndex");
            rssItemIndex = savedInstanceState.getInt("rssItemIndex");
            rssItemSentenceIndex = savedInstanceState.getInt("rssItemSentenceIndex");
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
        outState.putInt("rssFeedIndex",rssFeedIndex);
        outState.putInt("rssItemIndex",rssItemIndex);
        outState.putInt("rssItemSentenceIndex",rssItemSentenceIndex);
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
        itemPlayback.stopSpeaking();
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

        int result = textToSpeech.setLanguage(Locale.ENGLISH);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(ID, "Language ENGLISH not supported!");
            return;
        }

        findViewById(R.id.previousFeed).setEnabled(true);
        findViewById(R.id.nextFeed).setEnabled(true);

        itemPlayback.setTextToSpeech(textToSpeech);
        itemPlayback.setSentenceIndex(rssItemSentenceIndex);
        itemPlayback.setItemPlaybackListener(new ItemPlaybackListener() {

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
                setStatusText("Stopped", index, total);
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
                        TextView textView = (TextView) findViewById(R.id.status);
                        textView.setText(status, TextView.BufferType.EDITABLE);
                        ProgressBar bar = (ProgressBar) findViewById(R.id.readProgress);
                        bar.setProgress(index);
                        bar.setMax(total);
                    }
                });
            }

        });

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
    
    // --- UI callbacks ---

    public void nextFeed(final View v) {
        rssFeedIndex++;
        if (rssFeedIndex >= urls.length)
            rssFeedIndex = 0;
        updateRSSItems();
    }

    public void previousFeed(final View v) {
        rssFeedIndex--;
        if (rssFeedIndex < 0)
            rssFeedIndex = urls.length - 1;
        updateRSSItems();
    }

    public void previous(final View v) {
        playbackPreviousItem();
    }

    public void next(final View v) {
        playbackNextItem();
    }

    public void playPause(final View v) {
        itemPlayback.toggleSpeaking();
    }
    
    // --- Others ---

    private void updateRSSItems() {
        final String url = urls[rssFeedIndex];

        itemPlayback.stopSpeaking();
        final int firstPoint = url.indexOf(".");
        final String all = url.substring(firstPoint + 1, url.indexOf("/", firstPoint + 1));
        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(all, TextView.BufferType.EDITABLE);

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
        rssItemSentenceIndex=0;
        setItemForPlayback();
    }

    private void playbackPreviousItem() {
        rssItemIndex--;
        rssItemSentenceIndex=0;
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
            itemPlayback.startSpeaking();
        }

        setPlaybackCurrentSentence(text);
        setTitle(title);
    }

    private void setTitle(String titleText) {
        TextView textView = (TextView) findViewById(R.id.rssItemTitle);
        textView.setText(titleText, TextView.BufferType.EDITABLE);
    }

    private void setPlaybackCurrentSentence(String text) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text, TextView.BufferType.EDITABLE);
    }

}
