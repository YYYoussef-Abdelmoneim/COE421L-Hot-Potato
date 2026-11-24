// Light Duel: both players play. Wait for the cue, then press your LEFT or RIGHT button first.
// Player 1 uses buttons 1 and 2. Player 2 uses buttons 3 and 4.
public class LightDuel extends MiniGame {
    // The side of each button on the box: blue = right, green = left, red = right, yellow = left
    private static final int[] SIDE = {1, 0, 1, 0};   // 0 = left, 1 = right
    private static final int LEFT_LAMPS = 10;          // green + yellow
    private static final int RIGHT_LAMPS = 5;          // blue + red

    private static final int RELEASE = 0;      // waiting for all buttons to be let go
    private static final int WAIT = 1;         // random wait before the cue
    private static final int CUE = 2;          // cue is showing, first correct press wins
    private static final int SHOW_POINT = 3;   // short pause after a point

    private int step = RELEASE;
    private long stepEnds;
    private int cueSide;
    private int[] points = new int[2];
    private String message = "";

    public LightDuel(GameEngine engine) {
        super(engine);
    }

    public boolean isTwoPlayer() {
        return true;
    }

    public void start(long now) {
        newPoint();
    }

    private void newPoint() {
        hardware.setLamps(0);
        hardware.setSide(0);
        step = RELEASE;
    }

    public void tick(long now) {
        if (step == RELEASE && !engine.anyButtonHeld()) {
            step = WAIT;
            stepEnds = now + 1000 + engine.getRandom().nextInt(2001); // 1 to 3 seconds
        } else if (step == WAIT && now >= stepEnds) {
            cueSide = engine.getRandom().nextInt(2);
            if (cueSide == 0) {
                hardware.setLamps(LEFT_LAMPS);
            } else {
                hardware.setLamps(RIGHT_LAMPS);
            }
            hardware.setSide(cueSide + 1);
            step = CUE;
            stepEnds = now + difficulty.duelWindow();
        } else if (step == CUE && now >= stepEnds) {
            givePoint(-1, now); // nobody pressed in time
        } else if (step == SHOW_POINT && now >= stepEnds) {
            int needed = difficulty.duelPointsToWin();
            if (points[0] >= needed) {
                winner = 0;
                finish(true);
            } else if (points[1] >= needed) {
                winner = 1;
                finish(true);
            } else {
                newPoint();
            }
        }
    }

    public void pressed(int button, long now) {
        int p = button / 2; // buttons 0 and 1 are Player 1, buttons 2 and 3 are Player 2
        if (step == WAIT && difficulty.duelPenalty()) {
            givePoint(1 - p, now); // pressed too early
        } else if (step == CUE) {
            if (SIDE[button] == cueSide) {
                givePoint(p, now);
            } else if (difficulty.duelPenalty()) {
                givePoint(1 - p, now); // pressed the wrong side
            }
        }
    }

    // p = player who gets the point, or -1 for no point
    private void givePoint(int p, long now) {
        hardware.setLamps(0);
        hardware.setSide(0);
        if (p >= 0) {
            points[p]++;
            hardware.playSound("HIT");
            message = "Point P" + (p + 1);
        } else {
            hardware.playSound("MISS");
            message = "No point";
        }
        step = SHOW_POINT;
        stepEnds = now + 1200;
    }

    public String lcdTop() {
        if (step == RELEASE) {
            return "Release buttons";
        } else if (step == WAIT) {
            return "Wait for cue";
        } else if (step == CUE) {
            return cueSide == 0 ? "PRESS LEFT" : "PRESS RIGHT";
        }
        return message;
    }

    public String lcdBottom() {
        if (step == CUE) {
            return cueSide == 0 ? "P1 GREEN P2 YEL" : "P1 BLUE P2 RED";
        }
        return "Duel " + points[0] + "-" + points[1] + " to " + difficulty.duelPointsToWin();
    }

    // Used by the tests
    public boolean isCueShowing() {
        return step == CUE;
    }

    public int getCueSide() {
        return cueSide;
    }
}
