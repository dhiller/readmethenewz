package de.zalando.hackweek.read_me_the_newz;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

    private static final String[] urls = new String[] {
        "http://rss.slashdot.org/Slashdot/slashdot",
        "http://www.google.com/alerts/feeds/10782259317798652848/4797091171555319245", // Zalando news feed
    };
    private static final String IDENTIFIER = "ReadNewz";

    private ArrayList<RssItem> rssItems;
    private int rssItemIndex = -1;
    private TextToSpeech textToSpeech;
    private boolean ttsEnabled;
    private ReadNewz.TTSUtteranceProgressListener ttsUtteranceProgressListener = new TTSUtteranceProgressListener(
            new ArrayList<String>());

    private final UUID uuid = UUID.randomUUID();
    private HashMap<String, String> ttsParams;

    class TTSUtteranceProgressListener extends UtteranceProgressListener {

        private final ArrayList<String> sentences;
        private int sentenceIndex = 0;
        private boolean shouldSpeak = true;

        TTSUtteranceProgressListener(final ArrayList<String> sentences) {
            this.sentences = sentences;
        }

        @Override
        public void onStart(final String utteranceId) {
            Log.d(IDENTIFIER, "onStart: " + utteranceId);
        }

        @Override
        public void onDone(final String utteranceId) {
            Log.d(IDENTIFIER, "onDone: " + utteranceId);
            if (shouldSpeak) {
                sentenceIndex++;
                speakCurrentSentence();
            }
        }

        @Override
        public void onError(final String utteranceId) {
            Log.d(IDENTIFIER, "onError: " + utteranceId);
        }

        public boolean isSpeaking() {
            return textToSpeech.isSpeaking();
        }

        public void speakCurrentSentence() {
            stopSpeaking();
            shouldSpeak = true;
            if (sentences.size() > sentenceIndex) {
                textToSpeech.speak(sentences.get(sentenceIndex), TextToSpeech.QUEUE_FLUSH, ttsParams);
            } else {
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
                        public void run() {
                            readNextItem();
                        }
                    });
            }
        }

        public void stopSpeaking() {
            shouldSpeak = false;
            if (isSpeaking()) {
                textToSpeech.stop();
            }
        }

    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Required for the TTSUtteranceProgressListener
        // see http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
        ttsParams = new HashMap<String, String>() {

            {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uuid.toString());
            }
        };
        textToSpeech = new TextToSpeech(this, this);
    }

    public void previous(final View v) {
        readPreviousItem();
    }

    public void next(final View v) {
        readNextItem();
    }

    public void playPause(final View v) {
        if (ttsUtteranceProgressListener.isSpeaking()) {
            ttsUtteranceProgressListener.stopSpeaking();
        } else {
            ttsUtteranceProgressListener.speakCurrentSentence();
        }
    }

    // TextToSpeech.OnInitListener

    /**
     * Called after initialization of TextToSpeech.
     *
     * @param  status
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

        updateRSSItems();
    }

    private void updateRSSItems() {

        Log.d(IDENTIFIER, "Starting fetch of rss items from " + urls[0]);

        URL current = null;
        try {
            current = new URL(urls[0]);
        } catch (MalformedURLException e) {
            Log.e(IDENTIFIER, "Failed to create URL from " + urls[0], e);
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
        updateText();
    }

    private void readNextItem() {
        rssItemIndex++;
        updateText();
    }

    private void readPreviousItem() {
        rssItemIndex--;
        updateText();
    }

    private void updateText() {

        String text = "No data";
        final boolean hasItem = rssItems != null && rssItemIndex >= 0 && rssItems.size() > rssItemIndex;
        if (hasItem) {
            final RssItem currentItem = rssItems.get(rssItemIndex);
            final String title = Jsoup.parse(currentItem.getTitle()).text();
            final String description = Jsoup.parse(currentItem.getDescription()).text();
            text = title + " - " + description;
            if (ttsEnabled) {
                final ArrayList<String> sentences = new ArrayList<String>();

                // TODO Add date of article
                sentences.add(title); // + " - " + new SimpleDateFormat("") currentItem.getPubDate());
                sentences.addAll(Arrays.asList(description.split("\\. ")));

                if (ttsUtteranceProgressListener != null) {
                    ttsUtteranceProgressListener.stopSpeaking();
                }

                ttsUtteranceProgressListener = new TTSUtteranceProgressListener(sentences);

                final int listenerSetResult = textToSpeech.setOnUtteranceProgressListener(ttsUtteranceProgressListener);
                Log.d(IDENTIFIER, "Result for setListener: " + listenerSetResult);
                ttsUtteranceProgressListener.speakCurrentSentence();
            }
        }

        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text, TextView.BufferType.EDITABLE);
    }

}
