package de.zalando.hackweek.read_me_the_newz;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import de.zalando.hackweek.read_me_the_newz.extract.rss.RssItem;
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
    private PauseType pauseType = PauseType.NONE;

    ItemPlayback() {
        // Required for SUtteranceProgressListener
        // see http://stackoverflow.com/questions/20296792/tts-utteranceprogresslistener-not-being-called
        ttsParams = new HashMap<String, String>() {
            {
                
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            }
        };
    }
    
    enum PauseType {
        AFTER_TITLE(750, true),
        AFTER_ITEM(1500, true),
        NONE(0, false),
        ;
        private final int duration;
        private final boolean issuePause;

        private PauseType(int duration, boolean issuePause) {
            this.duration = duration;
            this.issuePause = issuePause;
        }

        private static PauseType getPauseTypeForIndexValue(int size, int sentenceIndex1) {
            final PauseType newPauseType;
            if(sentenceIndex1 ==0) {
                newPauseType = AFTER_TITLE;
            } else
            if(sentenceIndex1== size -1) {
                newPauseType = AFTER_ITEM;
            } else {
                newPauseType = NONE;
            }
            return newPauseType;
        }

        public void issuePause(TextToSpeech tts, HashMap<String,String> arguments) {
            tts.playSilence(duration,TextToSpeech.QUEUE_FLUSH,arguments);
        }

        public boolean isIssuePause() {
            return issuePause;
        }
        
    }

    public void setTextToSpeech(TextToSpeech textToSpeech) {
        this.textToSpeech = textToSpeech;
        @SuppressWarnings("deprecation") // UtteranceProgressListener is API level 15
        final int listenerSetResult = textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(final String utteranceId) {
                Log.d("TextToSpeech.OnUtteranceCompletedListener",String.format("utteranceId: %s",utteranceId));
                if(!ItemPlayback.this.utteranceId.equals(utteranceId))
                    return;
                
                if (pauseType.isIssuePause()) {
                    pauseType.issuePause(ItemPlayback.this.textToSpeech, ttsParams);
                    pauseType = PauseType.NONE;
                    return;
                }
                
                itemPlaybackListener.finishedItem(sentenceIndex, numberOfSentences(), currentSentence);
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
            pauseType = PauseType.getPauseTypeForIndexValue(sentences.size(), sentenceIndex);
        } else {
            itemPlaybackListener.finishedAll(numberOfSentences());
        }
    }

    public void stopSpeaking() {
        shouldSpeak = false;
        pauseType = PauseType.NONE;
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

    public void setItemForPlayback(RssItem rssItemForPlayback) {
        final ArrayList<String> sentences = new ArrayList<String>();
        sentences.add(getArticleTitle(rssItemForPlayback));
        sentences.addAll(getArticleSentences(rssItemForPlayback));
        this.setSentences(sentences);
    }

    private List<String> getArticleSentences(RssItem rssItemForPlayback) {
        return splitIntoSentences(sanitize(rssItemForPlayback.getDescription()));
    }

    private String getArticleTitle(RssItem rssItemForPlayback) {
        return getArticleSource(rssItemForPlayback) + sanitize(rssItemForPlayback.getTitle());
    }

    private String getArticleSource(RssItem rssItemForPlayback) {
        final String marker = sanitize(rssItemForPlayback.getMarker());
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
        return result.replaceAll("'([^ ]+)'", "\"$1\"");
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
