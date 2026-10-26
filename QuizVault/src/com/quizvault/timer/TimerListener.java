package com.quizvault.timer;

/**
 * Listener interface for timer events.
 *
 * JAVA TOPICS: Interface
 */
public interface TimerListener {
    void onTick(int secondsRemaining);
    void onFinish();
}
