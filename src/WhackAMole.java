// Whack-a-Mole: press the lit button before time runs out.
// There are up to 8 targets. You pass with enough hits. Three misses end the game early.
public class WhackAMole extends MiniGame {
    private static final int WAIT = 0;          // short pause before the next target
    private static final int TARGET = 1;        // a lamp is lit, waiting for a press
    private static final int SHOW_RESULT = 2;   // short pause after a hit or a miss

    private int step = WAIT;
    private long stepEnds;
    private int target;
    private int targets = 0;
    private int hits = 0;
    private int misses = 0;

    public WhackAMole(GameEngine engine) {
        super(engine);
    }

    public void start(long now) {
        hardware.setLamps(0);
        step = WAIT;
        stepEnds = now + 650;
    }

    public void tick(long now) {
        if (step == WAIT && now >= stepEnds && !engine.anyButtonHeld()) {
            // Light one random button
            target = engine.getRandom().nextInt(4);
            targets++;
            hardware.setLamps(1 << target);
            step = TARGET;
            stepEnds = now + difficulty.whackTime(engine.getChallengesDone());
        } else if (step == TARGET && now >= stepEnds) {
            result(false, now); // too slow
        } else if (step == SHOW_RESULT && now >= stepEnds) {
            if (misses >= 3 || targets >= 8) {
                finish(hits >= difficulty.whackHitsNeeded());
            } else {
                step = WAIT;
                stepEnds = now + 650;
            }
        }
    }

    public void pressed(int button, long now) {
        if (step == TARGET) {
            result(button == target, now);
        }
    }

    private void result(boolean hit, long now) {
        hardware.setLamps(0);
        if (hit) {
            hits++;
            hardware.playSound("HIT");
        } else {
            misses++;
            hardware.playSound("MISS");
        }
        step = SHOW_RESULT;
        stepEnds = now + 700;
    }

    public String lcdTop() {
        if (step == TARGET) {
            return player + " press " + COLORS[target];
        }
        return player + " get ready";
    }

    public String lcdBottom() {
        return "Hits " + hits + "/" + difficulty.whackHitsNeeded();
    }

    // Used by the tests
    public boolean isTargetLit() {
        return step == TARGET;
    }

    public int getTarget() {
        return target;
    }
}
