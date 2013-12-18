package de.zalando.hackweek.read_me_the_newz;

import android.speech.tts.TextToSpeech;
import nl.matshofman.saxrssreader.RssItem;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
* @author dhiller
*/
class ItemPlayback {

    private ArrayList<String> sentences;
    private int sentenceIndex = 0;
    private boolean shouldSpeak = true;
    private final UUID uuid = UUID.randomUUID();
    private HashMap<String, String> ttsParams;
    private TextToSpeech textToSpeech;
    private ItemPlaybackListener itemPlaybackListener = new ItemPlaybackListener();
    private String currentSentence;

    ItemPlayback() {
        // Required for SUtteranceProgressListener
        // see http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
        ttsParams = new HashMap<String, String>() {

            {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uuid.toString());
            }
        };
    }

    public void setTextToSpeech(TextToSpeech textToSpeech) {
        this.textToSpeech = textToSpeech;
    }

    public void setItemPlaybackListener(ItemPlaybackListener itemPlaybackListener) {
        if(itemPlaybackListener ==null)
            throw new IllegalArgumentException(("itemPlaybackListener is null!"));
        this.itemPlaybackListener = itemPlaybackListener;
    }

    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }

    public void continueWithNextSentence() {
        if (!shouldSpeak)
            return;
        itemPlaybackListener.finishedItem(sentenceIndex, numberOfSentences(), currentSentence);
        sentenceIndex++;
        startSpeaking();
    }

    public void startSpeaking() {
        stopSpeaking();
        shouldSpeak = true;
        if (numberOfSentences() > sentenceIndex) {
            currentSentence = sentences.get(sentenceIndex);
            textToSpeech.speak(currentSentence, TextToSpeech.QUEUE_FLUSH, ttsParams);
            itemPlaybackListener.beganWith(sentenceIndex, numberOfSentences(), currentSentence);
        } else {
            itemPlaybackListener.finishedAll(numberOfSentences());
        }
    }

    public void stopSpeaking() {
        shouldSpeak = false;
        if (isSpeaking()) {
            textToSpeech.stop();
            itemPlaybackListener.stoppedAt(sentenceIndex, numberOfSentences(), currentSentence);
        }
    }

    public int numberOfSentences() {
        return (sentences!=null?sentences.size():0);
    }

    public void toggleSpeaking() {
        if (isSpeaking()) {
            stopSpeaking();
        } else {
            startSpeaking();
        }
    }

    public void setItemForPlayback(RssItem itemForPlayback) {
        final String title = Jsoup.parse(itemForPlayback.getTitle()).text();
        final String description = Jsoup.parse(itemForPlayback.getDescription()).text();
        final ArrayList<String> sentences = new ArrayList<String>();

        // TODO Add date of article
        sentences.add(title);
        final List<String> lines = Arrays.asList(description.split("\\. "));
        sentences.addAll(lines);
        
        this.setSentences(sentences);
    }

    public void setSentenceIndex(int sentenceIndex) {
        this.sentenceIndex = sentenceIndex;
    }

    private void setSentences(ArrayList<String> sentences) {
        stopSpeaking();
        this.sentences = sentences;
        this.sentenceIndex = 0;
    }

    public String getCurrentSentence() {
        return this.sentences.get(sentenceIndex);
    }
}
