package dev.nebula.client.utils.helper;

import java.util.concurrent.ThreadLocalRandom;

public class TimerHelper {

    private long lastMS = 0L;

    public TimerHelper() {
        reset();
    }

    public boolean isDelayComplete(double minCPS, double maxCPS) {
        if (minCPS <= 0 || maxCPS <= 0 || maxCPS < minCPS) return true;

        double randomCPS = minCPS + ThreadLocalRandom.current().nextDouble() * (maxCPS - minCPS);
        long delay = (long) (1000.0 / randomCPS); 

        return getCurrentMS() - lastMS >= delay;
    }

    public boolean hasTimePassed(long milliseconds) {
        return getCurrentMS() - lastMS >= milliseconds;
    }

    public void reset() {
        lastMS = getCurrentMS();
    }

    public void setDelay(long milliseconds) {
        lastMS = getCurrentMS() - (1000 - milliseconds);
    }

    private long getCurrentMS() {
        return System.nanoTime() / 1_000_000L;
    }

    public boolean hasReached(long delayMs) {
        return System.currentTimeMillis() - lastMS >= delayMs;
    }
}
