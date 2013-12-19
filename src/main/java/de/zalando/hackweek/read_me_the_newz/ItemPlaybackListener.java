package de.zalando.hackweek.read_me_the_newz;

/**
 * @author dhiller
 */
class ItemPlaybackListener {
    void beganWith(int index, int total, String sentence) {}
    void stoppedAt(int index, int total, String sentence) {}
    void finishedItem(int index, int total, String sentence) {}
    void finishedAll(int total) {}
}
