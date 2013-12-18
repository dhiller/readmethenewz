package de.zalando.hackweek.read_me_the_newz;

import android.speech.tts.TextToSpeech;
import de.zalando.hackweek.read_me_the_newz.rss.item.Item;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            textToSpeech.speak(splitAcronyms(currentSentence), TextToSpeech.QUEUE_FLUSH, ttsParams);
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

    public void setItemForPlayback(Item itemForPlayback) {
        final String title = Jsoup.parse(itemForPlayback.getTitle()).text();
        final String description = Jsoup.parse(itemForPlayback.getDescription()).text();
        final ArrayList<String> sentences = new ArrayList<String>();

        // TODO Add date of article
        sentences.add(title);
        sentences.addAll(splitIntoSentences(description));
        
        this.setSentences(sentences);
    }

    private List<String> splitIntoSentences(String description) {
        return Arrays.asList(description.replaceAll("([\\.?!]\"?) ", "$1\n").split("\n"));
    }

    private String splitAcronyms(String s) {
        String result = s;
        final Matcher matcher = Pattern.compile("([A-Z]{2,})").matcher(s);
        while (matcher.find()) {
            final String group = matcher.group(0);
            StringBuilder replacement = new StringBuilder();
            for (int index = 0; index < group.length(); index++) {
                replacement.append(" " + group.substring(index, index + 1));
            }
            result = result.replace(group, replacement.toString().substring(1));
        }
        return result;
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
