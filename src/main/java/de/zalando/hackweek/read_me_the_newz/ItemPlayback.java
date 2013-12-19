package de.zalando.hackweek.read_me_the_newz;

import android.speech.tts.TextToSpeech;
import android.util.Log;
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
    
    private static final String ID = "ItemPlayback";

    private ArrayList<String> sentences;
    private int sentenceIndex = 0;
    private boolean shouldSpeak = true;
    private final String utteranceId = UUID.randomUUID().toString();
    private HashMap<String, String> ttsParams;
    private TextToSpeech textToSpeech;
    private ItemPlaybackListener itemPlaybackListener = new ItemPlaybackListener();
    private String currentSentence;

    ItemPlayback() {
        // Required for SUtteranceProgressListener
        // see http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
        ttsParams = new HashMap<String, String>() {
            {
                
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            }
        };
    }

    public void setTextToSpeech(TextToSpeech textToSpeech) {
        this.textToSpeech = textToSpeech;
        @SuppressWarnings("deprecation") // UtteranceProgressListener is API level 15
        final int listenerSetResult = textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(final String utteranceId) {
                if(!ItemPlayback.this.utteranceId.equals(utteranceId))
                    return;
                continueWithNextSentence();
            }
        });
        Log.d(ID, "Result for setListener: " + listenerSetResult);
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
            textToSpeech.speak(improveForPlayback(currentSentence), TextToSpeech.QUEUE_FLUSH, ttsParams);
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
        final ArrayList<String> sentences = new ArrayList<String>();
        sentences.add(getArticleTitle(itemForPlayback));
        sentences.addAll(getArticleSentences(itemForPlayback));
        this.setSentences(sentences);
    }

    private List<String> getArticleSentences(Item itemForPlayback) {
        return splitIntoSentences(sanitize(itemForPlayback.getDescription()));
    }

    private String getArticleTitle(Item itemForPlayback) {
        return getArticleSource(itemForPlayback) + sanitize(itemForPlayback.getTitle());
    }

    private String getArticleSource(Item itemForPlayback) {
        final String marker = sanitize(itemForPlayback.getMarker());
        if (marker.isEmpty())
            return "";
        final int dotIndex = marker.indexOf(".");
        return (dotIndex < 0 ? marker : marker.substring(0, dotIndex)) + ": ";
    }

    private String sanitize(String text) {
        return Jsoup.parse(text).text();
    }

    private List<String> splitIntoSentences(String description) {
        return Arrays.asList(description.replaceAll("([\\.?!][\"']?) ", "$1\n").split("\n"));
    }

    private String improveForPlayback(String s) {
        return replaceTrailingHyphensWithSpaces(replaceSingleQuotedTermsWithDoubleQuoted(replaceAcronymsWithDottedUppercaseChars(s)));
    }

    private String replaceTrailingHyphensWithSpaces(String text) {
        return text.replaceAll("([^-])-", "$1 ");
    }

    private String replaceSingleQuotedTermsWithDoubleQuoted(String result) {
        return result.replaceAll("'([^ ]+)'","\"$1\"");
    }

    private String replaceAcronymsWithDottedUppercaseChars(String s) {
        String result = s;
        final Matcher matcher = Pattern.compile("([A-Z]{2,})").matcher(s);
        while (matcher.find()) {
            final String group = matcher.group(0);
            StringBuilder replacement = new StringBuilder();
            for (int index = 0; index < group.length(); index++) {
                replacement.append(group.substring(index, index + 1)).append(".");
            }
            result = result.replace(group, replacement.toString());
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
