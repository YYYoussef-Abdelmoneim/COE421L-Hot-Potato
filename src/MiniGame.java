// The base class for the three mini-games.
// Each game fills in the abstract methods in its own way (inheritance and polymorphism).
public abstract class MiniGame {
    // Button order on the main board: 0 blue, 1 green, 2 red, 3 yellow
    public static final String[] COLORS = {"BLUE", "GREEN", "RED", "YELLOW"};

    protected GameEngine engine;
    protected Hardware hardware;
    protected DifficultyStrategy difficulty;
    protected String player;          // "P1" or "P2", the player with the potato

    private boolean finished = false;
    private boolean passed = false;   // used by one-player games
    protected int winner = -1;        // used by Light Duel: 0 = P1, 1 = P2

    public MiniGame(GameEngine engine) {
        this.engine = engine;
        hardware = engine.getHardware();
        difficulty = engine.getDifficulty();
        player = "P" + (engine.getHolder() + 1);
    }

    public abstract void start(long now);

    // Called when a button is pressed (not when it is released).
    public abstract void pressed(int button, long now);

    // Called about 20 times a second so the game can move on.
    public abstract void tick(long now);

    public abstract String lcdTop();

    public abstract String lcdBottom();

    // Light Duel overrides this. Two-player games skip the light sensor rule.
    public boolean isTwoPlayer() {
        return false;
    }

    protected void finish(boolean success) {
        finished = true;
        passed = success;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isPassed() {
        return passed;
    }

    public int getWinner() {
        return winner;
    }
}
