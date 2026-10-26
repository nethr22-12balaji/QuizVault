package com.quizvault.timer;

/**
 * A background thread that counts down the time for a question.
 *
 * JAVA TOPICS: Threads, Exception Handling
 */
public class QuizTimer extends Thread {
    private int seconds;
    private TimerListener listener;
    private volatile boolean running;

    public QuizTimer(int seconds, TimerListener listener) {
        this.seconds = seconds;
        this.listener = listener;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            while (seconds > 0 && running) {
                if (listener != null) {
                    listener.onTick(seconds);
                }
                Thread.sleep(1000); // 1 second
                seconds--;
            }
            if (running && listener != null) {
                listener.onFinish();
            }
        } catch (InterruptedException e) {
            System.out.println("Timer interrupted.");
        }
    }

    public void stopTimer() {
        running = false;
        this.interrupt();
    }
}
