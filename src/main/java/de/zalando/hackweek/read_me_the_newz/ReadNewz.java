package de.zalando.hackweek.read_me_the_newz;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.widget.ProgressBar;
import org.jsoup.Jsoup;

import org.xml.sax.SAXException;

import android.app.Activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.util.Log;

import android.view.View;

import android.widget.TextView;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class ReadNewz extends Activity implements TextToSpeech.OnInitListener {

    private static final String[] urls = new String[]{
            "http://rss.slashdot.org/Slashdot/slashdot",
            "http://www.google.com/alerts/feeds/10782259317798652848/4797091171555319245", // Zalando news feed
    };
    private static final String IDENTIFIER = "ReadNewz";

    private ArrayList<RssItem> rssItems;
    private int rssItemIndex = -1;
    private TextToSpeech textToSpeech;
    private boolean ttsEnabled;
    private final ItemPlayback itemPlayback = new ItemPlayback();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        itemPlayback.stopSpeaking();
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

    // TextToSpeech.OnInitListener

    /**
     * Called after initialization of TextToSpeech.
     *
     * @param status
     */
    @Override
    public void onInit(final int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(IDENTIFIER, "TextToSpeech Init failed!");
            return;
        }

        int result = textToSpeech.setLanguage(Locale.ENGLISH);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(IDENTIFIER, "Language ENGLISH not supported!");
            return;
        }

        ttsEnabled = true;
        
        itemPlayback.setTextToSpeech(textToSpeech);
        itemPlayback.setItemPlaybackListener(new ItemPlaybackListener() {

            @Override
            void beganWith(int index, int total, final String sentence) {
                setStatusText("Reading", index, total);
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
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
                    public void run() {
                        playbackNextItem();
                    }
                });
            }

            private void setStatusText(final String status, final int index, final int total) {
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
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

        final int listenerSetResult = textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener(){

            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                itemPlayback.continueWithNextSentence();
            }

            @Override
            public void onError(String utteranceId) {}
        });
        Log.d(IDENTIFIER, "Result for setListener: " + listenerSetResult);
        
        updateRSSItems();
    }

    private void updateRSSItems() {

        final String url = urls[0];
        final int firstPoint = url.indexOf(".");
        final String all = url.substring(firstPoint + 1, url.indexOf("/", firstPoint + 1));
        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(all, TextView.BufferType.EDITABLE);

        Log.d(IDENTIFIER, "Starting fetch of rss items from " + url);

        URL current = null;
        try {
            current = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(IDENTIFIER, "Failed to create URL from " + url, e);
        }

        try {
            rssItems = new AsyncTask<URL, Integer, ArrayList<RssItem>>() {
                @Override
                protected ArrayList<RssItem> doInBackground(final URL... params) {
                    final ArrayList<RssItem> result = new ArrayList<RssItem>();
                    for (URL u : params) {
                        Log.d(IDENTIFIER, "doInBackground: Starting fetch of rss items from " + u);

                        try {
                            final RssFeed feed = RssReader.read(u);
                            Log.d(IDENTIFIER, "Read items from " + u);

                            final ArrayList<RssItem> items = feed.getRssItems();
                            Log.d(IDENTIFIER, "Got " + items.size() + " items from " + u);
                            result.addAll(items);
                        } catch (SAXException e) {
                            Log.e(IDENTIFIER, "Failed to parse rss items from " + u, e);
                        } catch (IOException e) {
                            Log.e(IDENTIFIER, "IOException from " + u, e);
                        }
                    }

                    return result;
                }
            }.execute(current).get();
        } catch (InterruptedException e) {
            Log.e(IDENTIFIER, "Failed to parse rss items from " + current, e);
        } catch (ExecutionException e) {
            Log.e(IDENTIFIER, "Failed to parse rss items from " + current, e);
        }

        rssItemIndex = 0;
        setItemForPlayback();
    }

    private void playbackNextItem() {
        rssItemIndex++;
        setItemForPlayback();
    }

    private void playbackPreviousItem() {
        rssItemIndex--;
        setItemForPlayback();
    }

    private void setItemForPlayback() {
        
        findViewById(R.id.previous).setEnabled(rssItemIndex > 0);
        findViewById(R.id.next).setEnabled(rssItemIndex < rssItems.size());
        findViewById(R.id.playPause).setEnabled(rssItems.size() > 0);

        String text = "No data";
        final boolean hasItem = rssItems != null && rssItemIndex >= 0 && rssItems.size() > rssItemIndex;
        if (hasItem) {
            final RssItem currentItem = rssItems.get(rssItemIndex);

            TextView textView = (TextView) findViewById(R.id.rssItemTitle);
            textView.setText(Jsoup.parse(currentItem.getTitle()).text(), TextView.BufferType.EDITABLE);

            text = Jsoup.parse(currentItem.getDescription()).text();
            itemPlayback.setItemForPlayback(currentItem);
            itemPlayback.startSpeaking();
        }

        setPlaybackCurrentSentence(text);
    }

    private void setPlaybackCurrentSentence(String text) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text, TextView.BufferType.EDITABLE);
    }

}
