// The hidden round timer. It can pause and resume.
public class PotatoTimer {
    private long length;      // full time for this round (ms)
    private long timeLeft;    // time left while paused (ms)
    private long endTime;     // clock time when it runs out, while running
    private boolean running = false;

    public void reset(long lengthMs) {
        length = lengthMs;
        timeLeft = lengthMs;
        running = false;
    }

    public void resume(long now) {
        if (!running) {
            endTime = now + timeLeft;
            running = true;
        }
    }

    public void pause(long now) {
        if (running) {
            timeLeft = getTimeLeft(now);
            running = false;
        }
    }

    public long getTimeLeft(long now) {
        if (running) {
            return Math.max(0, endTime - now);
        }
        return timeLeft;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isExpired(long now) {
        return running && now >= endTime;
    }

    // Time until the next tick sound: 1.6 s when the timer is full, 0.24 s when it is almost empty.
    public long tickGap(long now) {
        return 240 + 1360 * getTimeLeft(now) / length;
    }
}
