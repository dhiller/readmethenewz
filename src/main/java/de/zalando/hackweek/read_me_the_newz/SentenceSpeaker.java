package de.zalando.hackweek.read_me_the_newz;

import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
* @author dhiller
*/
class SentenceSpeaker {

    private ArrayList<String> sentences;
    private int sentenceIndex = 0;
    private boolean shouldSpeak = true;
    private final UUID uuid = UUID.randomUUID();
    private HashMap<String, String> ttsParams;
    private TextToSpeech textToSpeech;
    private SentenceSpeakerListener sentenceSpeakerListener = new SentenceSpeakerListener();

    SentenceSpeaker() {
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

    public void setSentenceSpeakerListener(SentenceSpeakerListener sentenceSpeakerListener) {
        if(sentenceSpeakerListener==null)
            throw new IllegalArgumentException(("sentenceSpeakerListener is null!"));
        this.sentenceSpeakerListener = sentenceSpeakerListener;
    }

    public boolean isSpeaking() {
        return textToSpeech.isSpeaking();
    }

    public void speakNextSentence() {
        if (!shouldSpeak)
            return;
        sentenceSpeakerListener.finishedItem(sentenceIndex, sentences.size());
        sentenceIndex++;
        speakCurrentSentence();
    }

    public void speakCurrentSentence() {
        stopSpeaking();
        shouldSpeak = true;
        if (sentences.size() > sentenceIndex) {
            textToSpeech.speak(sentences.get(sentenceIndex), TextToSpeech.QUEUE_FLUSH, ttsParams);
            sentenceSpeakerListener.beganWith(sentenceIndex, sentences.size());
        } else {
            sentenceSpeakerListener.finishedAll(sentences.size());
        }
    }

    public void stopSpeaking() {
        shouldSpeak = false;
        if (isSpeaking()) {
            textToSpeech.stop();
            sentenceSpeakerListener.stoppedAt(sentenceIndex, sentences.size());
        }
    }

    public void setSentences(ArrayList<String> sentences) {
        stopSpeaking();
        this.sentences = sentences;
        this.sentenceIndex = 0;
    }

    public void toggleSpeaking() {
        if (isSpeaking()) {
            stopSpeaking();
        } else {
            speakCurrentSentence();
        }
    }

}
