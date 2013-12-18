package de.zalando.hackweek.read_me_the_newz;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Locale;

import org.jsoup.Jsoup;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import android.widget.ProgressBar;
import android.widget.TextView;

import android.view.View;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class ReadNewz extends Activity implements TextToSpeech.OnInitListener {

    private static final String[] FEED_URLS = {
        "http://rss.slashdot.org/Slashdot/slashdot",
        //            "http://www.google.com/alerts/feeds/10782259317798652848/4797091171555319245", // Zalando news feed
        "http://feeds.wired.com/wired/index",
    };
    private static final String ID = "ReadNewz";

    private final ItemPlayback itemPlayback = new ItemPlayback();

    private TextToSpeech textToSpeech;

    private AsyncTask<?, ?, ?> activeItemFetcher;

    private ArrayList<RssItem> rssItems;
    private int rssFeedIndex = 0;
    private int rssItemIndex = 0;
    private int rssItemSentenceIndex = 0;
    private boolean shouldSpeak = true;

    // --- Application lifecycle callbacks ---

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(ID, "onCreate");
        setContentView(R.layout.main);

        if (savedInstanceState != null) {
            rssFeedIndex = savedInstanceState.getInt("rssFeedIndex");
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
        if (activeItemFetcher != null) {
            activeItemFetcher.cancel(true);
        }
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
        if (rssFeedIndex >= FEED_URLS.length)
            rssFeedIndex = 0;
        updateRSSItems();
    }

    public void previousFeed(final View v) {
        rssFeedIndex--;
        if (rssFeedIndex < 0)
            rssFeedIndex = FEED_URLS.length - 1;

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
        final String url = FEED_URLS[rssFeedIndex];

        URL current = null;
        try {
            current = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(ID, "Failed to create URL from " + url, e);
            return;
        }

        itemPlayback.stopSpeaking();

        final int firstPoint = url.indexOf(".");
        final String all = url.substring(firstPoint + 1, url.indexOf("/", firstPoint + 1));
        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(all, TextView.BufferType.EDITABLE);

        setPlaybackCurrentSentence("");

        if (activeItemFetcher != null) {
            activeItemFetcher.cancel(true);
        }

        activeItemFetcher = new DownloadTask(this) {
            @Override
            protected void onProgressUpdate(final Progress... values) {
                final Progress progress = values[0];

                if (progress.isTotalBytesAvailable()) {
                    Log.i(ID, progress.getBytesReceived() + "/"+ progress.getTotalBytes());
                    setStatusText(format("Downloaded %s%%…", progress.getPercentage()),
                            (int) progress.getBytesReceived(), (int) progress.getTotalBytes());
                } else {
                    setStatusText(format("Downloaded %d bytes…", progress.getBytesReceived()), 0, 1);
                }
            }

            @Override
            protected void onPostExecute(final Result result) {
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

                setStatusText("Processing RSS feed…", 1, 1);
                activeItemFetcher = new AsyncTask<Void, Void, RssFeed>() {
                    @Override
                    protected RssFeed doInBackground(final Void... unused) {
                        try {
                            return RssReader.read(new ByteArrayInputStream(result.getContent()));
                        } catch (Exception e) {
                            Log.e(ID, "RSS parsing failed", result.getException());
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(final RssFeed result) {
                        if (activeItemFetcher != this) {
                            return;
                        }

                        activeItemFetcher = null;

                        if (isCancelled()) {
                            return;
                        }

                        setStatusText("", 0, 1);

                        rssItems = result.getRssItems();
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
        }.execute(current);
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
            if(shouldSpeak)
                itemPlayback.startSpeaking();
        }

        setPlaybackCurrentSentence(text);
        setTitle(title);
        setStatusText(shouldSpeak?"Reading":"Paused",rssItemSentenceIndex,itemPlayback.numberOfSentences());
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

}
