// Memory: watch a pattern of colors, then repeat it with the buttons.
// The pattern gets longer as the round goes on.
public class MemoryGame extends MiniGame {
    private static final int SHOW_GAP = 0;       // short dark gap between colors
    private static final int SHOW_COLOR = 1;     // one color is lit
    private static final int WAIT_RELEASE = 2;   // waiting for all buttons to be let go
    private static final int ANSWER = 3;         // the player repeats the pattern

    private int[] pattern;
    private int position = 0;   // which color we are showing or expecting
    private int step = SHOW_GAP;
    private long stepEnds;

    public MemoryGame(GameEngine engine) {
        super(engine);
    }

    public void start(long now) {
        int length = difficulty.memoryStartColors() + Math.min(5, engine.getChallengesDone());
        pattern = new int[length];
        for (int i = 0; i < length; i++) {
            pattern[i] = engine.getRandom().nextInt(4);
        }
        hardware.setLamps(0);
        step = SHOW_GAP;
        stepEnds = now + 500;
    }

    public void tick(long now) {
        if (step == SHOW_GAP && now >= stepEnds) {
            if (position == 0 && engine.anyButtonHeld()) {
                return; // wait until the player lets go of the buttons
            }
            hardware.setLamps(1 << pattern[position]);
            step = SHOW_COLOR;
            stepEnds = now + difficulty.memoryFlashTime();
        } else if (step == SHOW_COLOR && now >= stepEnds) {
            hardware.setLamps(0);
            position++;
            if (position == pattern.length) {
                step = WAIT_RELEASE;
            } else {
                step = SHOW_GAP;
                stepEnds = now + 300;
            }
        } else if (step == WAIT_RELEASE && !engine.anyButtonHeld()) {
            position = 0;
            step = ANSWER;
            stepEnds = now + difficulty.memoryAnswerTime(pattern.length);
        } else if (step == ANSWER && now >= stepEnds) {
            finish(false); // too slow
        }
    }

    public void pressed(int button, long now) {
        if (step != ANSWER) {
            return;
        }
        if (button == pattern[position]) {
            position++;
            hardware.playSound("HIT");
            if (position == pattern.length) {
                finish(true);
            }
        } else {
            finish(false); // wrong color
        }
    }

    public String lcdTop() {
        if (step == ANSWER) {
            return player + " repeat " + position + "/" + pattern.length;
        }
        return player + " watch";
    }

    public String lcdBottom() {
        if (step == ANSWER) {
            return "Your turn!";
        }
        return pattern.length + " colors";
    }

    // Used by the tests
    public boolean isAnswering() {
        return step == ANSWER;
    }

    public int[] getPattern() {
        return pattern;
    }
}
