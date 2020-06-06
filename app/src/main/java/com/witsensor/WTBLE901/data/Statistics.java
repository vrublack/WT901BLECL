package com.witsensor.WTBLE901.data;

public class Statistics {
    private long start = System.currentTimeMillis();
    private long lastPaused = -1;
    private long timePaused = 0;
    private int receivedSamples = 0;

    public void resume() {
        if (lastPaused != -1) {
            timePaused += System.currentTimeMillis() - lastPaused;
        }

        lastPaused = -1;
    }

    public void pause() {
        lastPaused = System.currentTimeMillis();
    }

    public long getRunningTimeMs() {
        return System.currentTimeMillis() - start - timePaused;
    }

    public void logSampleReceived() {
        receivedSamples++;
    }

    public double getSamplesPerSecond() {
        return receivedSamples / ((double) getRunningTimeMs() / 1000);
    }
}
