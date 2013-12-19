package de.zalando.hackweek.read_me_the_newz;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

public class ReadNewz extends Activity implements AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnInitListener {

    private static final String ID = "ReadNewz";

    private final ItemPlayback itemPlayback = new ItemPlayback();
    private final ItemPlaybackFeedBackProvider itemPlaybackFeedBackProvider = new ItemPlaybackFeedBackProvider();

    private final MediaButtonReceiver mediaButtonReceiver = new MediaButtonReceiver();
    private AudioManager audioManager;
    private ComponentName remoteControlReceiver;
    private boolean hasAudioFocus;

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

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceiver(mediaButtonReceiver, new IntentFilter(Intents.MEDIA_BUTTONS));
        remoteControlReceiver = new ComponentName(this, RemoteControlReceiver.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        requestAudioFocus();
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
        unregisterReceiver(mediaButtonReceiver);
        abandonAudioFocus();
    }

    // --- AudioManager.OnAudioFocusChangeListener ---

    @Override
    public void onAudioFocusChange(final int focusChange) {

        final boolean newAudioFocus;
        switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN :
            Log.d(ID, "Regained audio focus");
            newAudioFocus = true;
            break;

        case AudioManager.AUDIOFOCUS_LOSS :
            Log.d(ID, "Lost audio focus");
            newAudioFocus = false;
            abandonAudioFocus();
            break;

        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT :
            Log.d(ID, "Ignoring transient audio focus loss");
            return;

        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK :
            Log.d(ID, "Ignoring transient, duckable audio focus loss");
            return;

        default :
            Log.i(ID, "Ignoring audio focus loss change event of type " + focusChange);
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAudioFocus(newAudioFocus);
            }
        });
    }

    private void requestAudioFocus() {
        Log.d(ID, "requestAudioFocus");

        // Request audio focus for playback
        final int result = audioManager.requestAudioFocus(this, //
                AudioManager.STREAM_MUSIC,   // Use the music stream.
                AudioManager.AUDIOFOCUS_GAIN // Request permanent focus.
                );

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            updateAudioFocus(true);
        }
    }

    private void abandonAudioFocus() {
        Log.d(ID, "abandonAudioFocus");
        if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            updateAudioFocus(false);
        }
    }

    private void updateAudioFocus(final boolean newAudioFocus) {
        if (newAudioFocus != hasAudioFocus) {
            hasAudioFocus = newAudioFocus;
            audioFocusUpdated();
        }
    }

    private void audioFocusUpdated() {
        if (hasAudioFocus) {
            audioManager.registerMediaButtonEventReceiver(remoteControlReceiver);

            if (shouldSpeak && !itemPlayback.isSpeaking()) {
                itemPlayback.startSpeaking();
            }
        } else {
            audioManager.unregisterMediaButtonEventReceiver(remoteControlReceiver);

            if (itemPlayback.isSpeaking()) {
                itemPlayback.stopSpeaking();
            }
        }
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

        itemPlayback.setTextToSpeech(textToSpeech);
        itemPlayback.setSentenceIndex(rssItemSentenceIndex);
        itemPlayback.setItemPlaybackListener(itemPlaybackFeedBackProvider);

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

    // --- Overrides ---

    // @Override
    // public boolean dispatchKeyEvent(KeyEvent event) {
    //     Log.d(ID, "dispatchKeyEvent: " + event.getKeyCode() + ", " + event.getAction());
    //     handleMediaKeyEvent(event);
    //     return super.dispatchKeyEvent(event);
    // }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getRepeatCount() == 0 && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            handleMediaKeyEvent(keyEvent.getKeyCode());
        }
    }

    private void handleMediaKeyEvent(final int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_PLAY :
            shouldSpeak = true;
            if (!itemPlayback.isSpeaking()) {
                itemPlayback.startSpeaking();
            }
            break;

        case KeyEvent.KEYCODE_MEDIA_STOP :
            shouldSpeak = false;
            itemPlayback.stopSpeaking();
            break;

        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE :
            playPause(null);
            break;

        case KeyEvent.KEYCODE_MEDIA_NEXT :
            playbackNextItem();
            break;

        case KeyEvent.KEYCODE_MEDIA_PREVIOUS :
            playbackPreviousItem();
            break;

        case KeyEvent.KEYCODE_HEADSETHOOK :
            shouldSpeak = false;
            itemPlayback.stopSpeaking();
            break;
        }
    }

    // --- UI callbacks ---

    @SuppressWarnings("UnusedParameters")
    public void nextFeed(final View v) {
        startPlaybackForNextFeed();
    }

    @SuppressWarnings("UnusedParameters")
    public void previousFeed(final View v) {
        startPlaybackForPreviousFeed();
    }

    @SuppressWarnings("UnusedParameters")
    public void previous(final View v) {
        playbackPreviousItem();
    }

    @SuppressWarnings("UnusedParameters")
    public void next(final View v) {
        playbackNextItem();
    }

    @SuppressWarnings("UnusedParameters")
    public void playPause(final View v) {
        shouldSpeak = !shouldSpeak;
        if (shouldSpeak) {
            if (hasAudioFocus) {
                itemPlayback.startSpeaking();
            }
        } else {
            itemPlayback.stopSpeaking();
        }
    }

    // --- Others ---

    private void startPlaybackForNextFeed() {
        setRssFeedIndex(rssFeedDescriptorIndex + 1);
        updateRSSItems();
    }

    private void startPlaybackForPreviousFeed() {
        setRssFeedIndex(rssFeedDescriptorIndex - 1);
        updateRSSItems();
    }

    private void setRssFeedIndex(int feedIndex) {
        rssFeedDescriptorIndex = feedIndex;
        if (rssFeedDescriptorIndex >= rssFeedDescriptors.size())
            rssFeedDescriptorIndex = 0;
        if (rssFeedDescriptorIndex < 0)
            rssFeedDescriptorIndex = rssFeedDescriptors.size() - 1;
        rssItemSentenceIndex = 0;
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
        if (rssItemIndex >= rssItems.size()) {
            startPlaybackForNextFeed();
        } else {
            setRssItemIndex(rssItemIndex + 1);
        }
    }

    private void playbackPreviousItem() {
        setRssItemIndex(rssItemIndex - 1);
    }

    private void setRssItemIndex(int newrssItemIndex) {
        rssItemIndex = newrssItemIndex;
        rssItemSentenceIndex = 0;
        itemPlayback.setSentenceIndex(rssItemSentenceIndex);
        setItemForPlayback();
    }

    private void setItemForPlayback() {

        updateButtonEnabledState();

        String title = "";
        String text = "";
        final boolean hasItem = rssItems != null && rssItemIndex >= 0 && rssItems.size() > rssItemIndex;
        if (hasItem) {
            final Item currentItem = rssItems.get(rssItemIndex);
            title = Jsoup.parse(currentItem.getTitle()).text();
            text = Jsoup.parse(currentItem.getDescription()).text();
            itemPlayback.setItemForPlayback(currentItem);
            itemPlayback.setSentenceIndex(rssItemSentenceIndex);
            if (shouldSpeak && hasAudioFocus) {
                itemPlayback.startSpeaking();
            }
        }

        setPlaybackCurrentSentence(text);
        setTitle(title);
        setStatusText(shouldSpeak ? "Reading" : "Paused", rssItemSentenceIndex, itemPlayback.numberOfSentences());
        setPlaybackCurrentSentence(itemPlayback.getCurrentSentence());
    }

    private void updateButtonEnabledState() {
        findViewById(R.id.previousFeed).setEnabled(rssFeedDescriptorIndex > 0);
        findViewById(R.id.nextFeed).setEnabled(rssFeedDescriptorIndex < rssFeedDescriptors.size());
        findViewById(R.id.previous).setEnabled(rssItemIndex > 0);
        findViewById(R.id.next).setEnabled(rssItemIndex < rssItems.size());
        findViewById(R.id.playPause).setEnabled(rssItems.size() > 0);
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

    static void postToMainLoop(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static final class Intents {
        public static final String MEDIA_BUTTONS = "de.zalando.hackweek.read_me_the_newz.ReadNewz:MEDIA_BUTTONS";

        private Intents() {
            // no instances, please
        }
    }

    /** Downloads a RSS feed, updating progress information in the UI. */
    private final class RssFeedDownloadTask extends DownloadTask<RssFeedDescriptor> {
        RssFeedDownloadTask() {
            super(ReadNewz.this);
        }

        @Override
        protected void onPreExecute() {
            if (activeItemFetcher != this) {
                return;
            }

            setStatusTextIndeterminate("Starting RSS feed download…");
        }

        @Override
        protected void onProgressUpdate(final Progress... values) {
            if (activeItemFetcher != this) {
                cancel(false);
                return;
            }

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
                setStatusText("RSS download failed.", 0, 1);
                return;
            }

            activeItemFetcher = new RssFeedParsingTask().execute(result);
        }

        @Override
        protected void onCancelled(final Result<RssFeedDescriptor> result) {
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

    /** Parses downloaded RSS content. */
    private final class RssFeedParsingTask extends AsyncTask<DownloadTask.Result<RssFeedDescriptor>, Void, List<Item>> {
        @Override
        protected void onPreExecute() {
            if (activeItemFetcher != this) {
                return;
            }

            setStatusTextIndeterminate("Processing RSS feed…");
        }

        @Override
        protected List<Item> doInBackground(final DownloadTask.Result<RssFeedDescriptor>... results) {

            final DownloadTask.Result<RssFeedDescriptor> result = results[0];
            final RssFeedDescriptor descriptor = result.getItem();

            try {
                final Source source = new Source(descriptor.getDescription(), descriptor.getDescription(), Type.RSS,
                        new URI(descriptor.getUrl()));
                return new ByteArrayContentProvider(source, result.getContent()).extract();
            } catch (Exception e) {
                Log.e(ID, "RSS parsing failed", result.getException());
                return Collections.emptyList();
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
            postToMainLoop(new Runnable() {
                @Override
                public void run() {
                    playbackNextItem();
                }
            });
        }

        private void setStatusText(final String status, final int index, final int total) {
            postToMainLoop(new Runnable() {
                @Override
                public void run() {
                    ReadNewz.this.setStatusText(status, index, total);
                }
            });
        }
    }

    /** Handles media button intents sent from {@link RemoteControlReceiver}. */
    private final class MediaButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Intents.MEDIA_BUTTONS.equals(intent.getAction())) {
                final KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d(ID, "Media button intent received: " + keyEvent);
                if (keyEvent != null) {
                    handleMediaKeyEvent(keyEvent);
                }
            }
        }
    }
}
