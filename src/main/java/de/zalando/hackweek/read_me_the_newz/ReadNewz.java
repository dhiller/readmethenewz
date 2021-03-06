package de.zalando.hackweek.read_me_the_newz;

import static java.lang.String.format;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;

import com.google.common.base.Throwables;

import de.zalando.hackweek.read_me_the_newz.extract.ByteArrayContentProvider;
import de.zalando.hackweek.read_me_the_newz.extract.feed.Feed;
import de.zalando.hackweek.read_me_the_newz.extract.feed.FeedItem;
import de.zalando.hackweek.read_me_the_newz.extract.feed.FeedSource;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

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

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import android.util.Log;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.ProgressBar;
import android.widget.TextView;

public class ReadNewz extends Activity implements AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnInitListener {

    private static final String ID = "ReadNewz";

    /**
     * ID of the ReadNewz notification for {@code NotificationManager}.
     */
    private static final int NOTIFICATION_ID = 1;

    private final ItemPlayback itemPlayback = new ItemPlayback();
    private final ItemPlaybackFeedBackProvider itemPlaybackFeedBackProvider = new ItemPlaybackFeedBackProvider();

    private final MediaButtonReceiver mediaButtonReceiver = new MediaButtonReceiver();
    private AudioManager audioManager;
    private ComponentName remoteControlReceiver;
    private boolean hasAudioFocus;

    private NotificationManager notificationManager;

    private TextToSpeech textToSpeech;
    private boolean shouldSpeak = true;

    private AsyncTask<?, ?, ?> activeAsyncTask;

    private List<FeedSource> feedSources;
    private int rssFeedDescriptorIndex = 0;

    private Feed rssFeedItems;
    private int rssItemIndex = 0;
    private int rssItemSentenceIndex = 0;

    // --- Application lifecycle callbacks ---

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(ID, "onCreate " + this);
        setContentView(R.layout.main);

        feedSources = FeedSource.getFeeds(getResources().getXml(R.xml.feedly_opml));

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
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        registerReceiver(mediaButtonReceiver, new IntentFilter(Intents.MEDIA_BUTTONS));
        remoteControlReceiver = new ComponentName(this, RemoteControlReceiver.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        requestAudioFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(ID, "onResume " + this);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(ID, "onSaveInstanceState " + this);
        outState.putInt("rssFeedIndex", rssFeedDescriptorIndex);
        outState.putInt("rssItemIndex", rssItemIndex);
        outState.putInt("rssItemSentenceIndex", rssItemSentenceIndex);
        outState.putBoolean("shouldSpeak", shouldSpeak);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(ID, "onPause " + this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(ID, "onStop " + this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(ID, "onRestart " + this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(ID, "onDestroy " + this);
        if (activeAsyncTask != null) {
            activeAsyncTask.cancel(true);
        }

        itemPlayback.stopSpeaking();
        unregisterReceiver(mediaButtonReceiver);
        abandonAudioFocus();

        cancelNotification();

        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    // --- AudioManager.OnAudioFocusChangeListener ---

    @Override
    public void onAudioFocusChange(final int focusChange) {

        final boolean newAudioFocus;
        switch (focusChange) {

            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(ID, "Regained audio focus");
                newAudioFocus = true;
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(ID, "Lost audio focus");
                newAudioFocus = false;
                abandonAudioFocus();
                break;

            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.d(ID, "Ignoring transient audio focus loss");
                return;

            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                Log.d(ID, "Ignoring transient, duckable audio focus loss");
                return;

            default:
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
     * @param  status  the initialization result
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

    public boolean setLanguage(final Locale language) {
        int result = textToSpeech.setLanguage(language);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(ID, "Language ENGLISH not supported!");
            return true;
        }

        return false;
    }

    // --- Overrides ---

    @Override
    protected void onNewIntent(final Intent intent) {
        Log.d(ID, "onNewIntent " + this + ", " + intent);
    }

    // @Override
    // public boolean dispatchKeyEvent(KeyEvent event) {
    // Log.d(ID, "dispatchKeyEvent: " + event.getKeyCode() + ", " + event.getAction());
    // handleMediaKeyEvent(event);
    // return super.dispatchKeyEvent(event);
    // }

    private void handleMediaKeyEvent(final KeyEvent keyEvent) {
        if (keyEvent.getRepeatCount() == 0 && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            handleMediaKeyEvent(keyEvent.getKeyCode());
        }
    }

    private void handleMediaKeyEvent(final int keyCode) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                shouldSpeak = true;
                if (!itemPlayback.isSpeaking()) {
                    itemPlayback.startSpeaking();
                }

                break;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                shouldSpeak = false;
                itemPlayback.stopSpeaking();
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playPause(null);
                break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playbackNextItem();
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playbackPreviousItem();
                break;

            case KeyEvent.KEYCODE_HEADSETHOOK:
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

    // -- Option Menu ---

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {

            case R.id.close:
                shouldSpeak = false;
                itemPlayback.stopSpeaking();
                abandonAudioFocus();
                cancelNotification();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // --- Notification ---

    private NotificationCompat.Builder createNotification(final CharSequence contentText) {

        // The contentIntent will be executed when the user clicks on the notification
        // This will be this activity
        final Intent contentIntent = new Intent(this, getClass());

        // Reuse a already running activity instead of launching a new one
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // The stack builder object will contain an artificial back stack for
        // the started Activity. This ensures that navigating backward from
        // the Activity leads out of your application to the Home screen.
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ReadNewz.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(contentIntent);

        final PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Several thins are required for a notification to work: small icon,
        // content intent, content title and content text. So require a
        // content text as function parameter in order to ensure that a
        // notification returned by this function is always displayable.

        return new NotificationCompat.Builder(this).setOngoing(true).setSmallIcon(R.drawable.app_logo)
                .setContentIntent(pendingIntent)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(contentText);
    }

    private void updateNotification(final Notification notification) {
        Log.d(ID, "updateNotification");
        notificationManager.notify(ID, NOTIFICATION_ID, notification);
    }

    private void cancelNotification() {
        notificationManager.cancel(ID, NOTIFICATION_ID);
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

    private void setRssFeedIndex(final int feedIndex) {
        rssFeedDescriptorIndex = feedIndex;
        if (rssFeedDescriptorIndex >= feedSources.size()) {
            rssFeedDescriptorIndex = 0;
        }

        if (rssFeedDescriptorIndex < 0) {
            rssFeedDescriptorIndex = feedSources.size() - 1;
        }

        rssItemSentenceIndex = 0;
    }

    private void updateRSSItems() {
        final FeedSource feedSource = feedSources.get(rssFeedDescriptorIndex);

        final Locale language = feedSource.getLanguage();
        if (setLanguage(language)) {
            setPlaybackCurrentSentence("Language " + language + "not supported!");
            return;
        }

        itemPlayback.stopSpeaking();

        TextView textView = (TextView) findViewById(R.id.rssHost);
        textView.setText(feedSource.longName(), TextView.BufferType.EDITABLE);

        setPlaybackCurrentSentence("");

        executeAsyncTask(new RssFeedDownloadTask(), feedSource);
    }

    private void playbackNextItem() {
        if (rssItemIndex >= rssFeedItems.size()) {
            startPlaybackForNextFeed();
        } else {
            setRssItemIndex(rssItemIndex + 1);
        }
    }

    private void playbackPreviousItem() {
        setRssItemIndex(rssItemIndex - 1);
    }

    private void setRssItemIndex(final int newrssItemIndex) {
        rssItemIndex = newrssItemIndex;
        rssItemSentenceIndex = 0;
        itemPlayback.setSentenceIndex(rssItemSentenceIndex);
        setItemForPlayback();
    }

    private void setItemForPlayback() {

        updateButtonEnabledState();

        String title = "";
        String text = "";
        final boolean hasItem = rssFeedItems != null && rssItemIndex >= 0 && rssFeedItems.size() > rssItemIndex;
        if (hasItem) {
            final FeedItem currentFeedItem = rssFeedItems.get(rssItemIndex);
            title = Jsoup.parse(currentFeedItem.getTitle()).text();
            text = Jsoup.parse(currentFeedItem.getDescription()).text();
            itemPlayback.setItemForPlayback(currentFeedItem);
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
        findViewById(R.id.nextFeed).setEnabled(rssFeedDescriptorIndex < feedSources.size());
        findViewById(R.id.previous).setEnabled(rssItemIndex > 0);
        findViewById(R.id.next).setEnabled(rssItemIndex < rssFeedItems.size());
        findViewById(R.id.playPause).setEnabled(rssFeedItems.size() > 0);
    }

    private void setTitle(final String titleText) {
        TextView textView = (TextView) findViewById(R.id.rssItemTitle);
        textView.setText(titleText, TextView.BufferType.EDITABLE);
    }

    private void setPlaybackCurrentSentence(final String text) {
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(text, TextView.BufferType.EDITABLE);
    }

    private void setStatusTextIndeterminate(final String status) {
        setStatusText(status, 0, 0);
    }

    private void setStatusText(final String status, final int index, final int total) {

        final NotificationCompat.Builder notificationBuilder = createNotification(status);

        final TextView textView = (TextView) findViewById(R.id.status);
        textView.setText(status, TextView.BufferType.EDITABLE);

        final ProgressBar bar = (ProgressBar) findViewById(R.id.readProgress);
        if (index == 0 && total == 0) {
            bar.setIndeterminate(true);
            bar.setProgress(0);
            bar.setMax(1);
        } else {
            bar.setIndeterminate(false);
            bar.setProgress(index);
            bar.setMax(total);
            notificationBuilder.setProgress(total, index, false);
        }

        updateNotification(notificationBuilder.build());
    }

    private <T> void executeAsyncTask(final AsyncTask<T, ?, ?> asyncTask, final T... params) {
        AsyncTask<?, ?, ?> oldTask = activeAsyncTask;
        activeAsyncTask = null;
        if (oldTask != null) {
            oldTask.cancel(true);
        }

        activeAsyncTask = asyncTask;
        asyncTask.execute(params);
    }

    public static final class Intents {
        public static final String MEDIA_BUTTONS = "de.zalando.hackweek.read_me_the_newz.ReadNewz:MEDIA_BUTTONS";

        private Intents() {
            // no instances, please
        }
    }

    /**
     * Downloads a feed, updating progress information in the UI.
     */
    private final class RssFeedDownloadTask extends DownloadTask<FeedSource> {
        RssFeedDownloadTask() {
            super(ReadNewz.this);
        }

        @Override
        protected void onPreExecute() {
            if (activeAsyncTask != this) {
                return;
            }

            setStatusTextIndeterminate("Starting feed download…");
        }

        @Override
        protected void onProgressUpdate(final Progress... values) {
            if (activeAsyncTask != this) {
                cancel(false);
                return;
            }

            final Progress progress = values[0];

            if (progress.isTotalBytesAvailable()) {
                Log.i(ID, progress.getBytesReceived() + "/" + progress.getTotalBytes());
                setStatusText(format("Downloaded %s%%…", progress.getPercentage()), (int) progress.getBytesReceived(),
                        (int) progress.getTotalBytes());
            } else {
                Log.i(ID, Long.toString(progress.getBytesReceived()));
                setStatusTextIndeterminate(format("Downloaded %d bytes…", progress.getBytesReceived()));
            }
        }

        @Override
        protected void onPostExecute(final Result<FeedSource> result) {
            if (activeAsyncTask != this) {
                return;
            }

            activeAsyncTask = null;

            if (isCancelled()) {
                return;
            }

            if (!result.isSuccess()) {
                Log.e(ID, "Feed download failed", result.getException());
                setStatusText("Feed download failed.", 0, 1);
                return;
            }

            executeAsyncTask(new RssFeedParsingTask(), result);
        }

        @Override
        protected void onCancelled(final Result<FeedSource> result) {
            if (activeAsyncTask == this) {
                activeAsyncTask = null;
            }
        }

        @Override
        protected URL getUrl(final FeedSource url) {
            try {
                return new URL(url.getUrl());
            } catch (MalformedURLException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    /**
     * Parses downloaded feed content.
     */
    private final class RssFeedParsingTask extends AsyncTask<DownloadTask.Result<FeedSource>, Void, Feed> {
        @Override
        protected void onPreExecute() {
            if (activeAsyncTask != this) {
                return;
            }

            setStatusTextIndeterminate("Processing feed…");
        }

        @Override
        protected Feed doInBackground(final DownloadTask.Result<FeedSource>... results) {

            final DownloadTask.Result<FeedSource> result = results[0];
            final FeedSource descriptor = result.getItem();

            try {
                return new ByteArrayContentProvider(descriptor, result.getContent()).extract();
            } catch (Exception e) {
                Log.e(ID, "Feed parsing failed", result.getException());
                return Feed.emptyFeed();
            }
        }

        @Override
        protected void onPostExecute(final Feed result) {
            if (activeAsyncTask != this) {
                return;
            }

            activeAsyncTask = null;

            if (isCancelled()) {
                return;
            }

            setStatusTextIndeterminate("");

            rssFeedItems = result;
            rssItemIndex = 0;
            setItemForPlayback();
        }

        @Override
        protected void onCancelled() {
            if (activeAsyncTask == this) {
                activeAsyncTask = null;
            }
        }
    }

    private class ItemPlaybackFeedBackProvider extends ItemPlaybackListener {

        @Override
        void beganWith(final int index, final int total, final String sentence) {
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
        void stoppedAt(final int index, final int total, final String sentence) {
            setStatusText("Paused", index, total);
        }

        @Override
        void finishedItem(final int index, final int total, final String sentence) {
            setStatusText("Finished", index, total);
        }

        @Override
        void finishedAll(final int total) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playbackNextItem();
                }
            });
        }

        private void setStatusText(final String status, final int index, final int total) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ReadNewz.this.setStatusText(status, index, total);
                }
            });
        }
    }

    /**
     * Handles media button intents sent from {@link RemoteControlReceiver}.
     */
    private final class MediaButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Intents.MEDIA_BUTTONS.equals(intent.getAction())) {
                final KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d(ID, "Media button intent received: " + keyEvent);
                if (keyEvent != null) {
                    handleMediaKeyEvent(keyEvent);
                }
            }
        }
    }
}
