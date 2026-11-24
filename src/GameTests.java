// Software tests for the game rules. They run without the box.
// Each test sends fake board messages to the engine and checks the result.
public class GameTests {
    private static int passed = 0;
    private static int failed = 0;
    private static long now;   // fake clock (ms)

    public static void main(String[] args) {
        cardPicksGame();
        unknownCard();
        whackPassMovesPotato();
        memoryMistakeCostsFuse();
        threeFailsLoseRound();
        duelWinnerTakesRound();
        liftingHandTwiceLosesRound();
        coverSensorToStart();
        timerRunsOut();
        hardModeChangesRules();
        System.out.println(passed + " checks passed, " + failed + " failed");
    }

    // Makes a new engine. The serial links are not opened, so nothing is really sent.
    private static GameEngine newEngine() {
        SerialLink link1 = new SerialLink();
        SerialLink link2 = new SerialLink();
        CardList cards = new CardList("rfid-cards.txt");
        cards.setCard(0, "AAAAAAAAAA");
        cards.setCard(1, "BBBBBBBBBB");
        cards.setCard(2, "CCCCCCCCCC");
        GameEngine engine = new GameEngine(new Hardware(link1, link2), cards);
        engine.setUseSensor(false);
        now = 0;
        engine.newMatch(now);
        return engine;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + name);
        }
    }

    // Moves the fake clock forward, calling tick every 10 ms.
    private static void wait(GameEngine engine, long ms) {
        for (long t = 0; t < ms; t += 10) {
            now += 10;
            engine.tick(now);
        }
    }

    private static void press(GameEngine engine, int button) {
        engine.receive("BTN " + (button + 1) + " DOWN", now);
        engine.receive("BTN " + (button + 1) + " UP", now);
    }

    private static void startGame(GameEngine engine, String card) {
        engine.receive("CARD " + card, now);
        press(engine, 0);
    }

    // Plays one Whack-a-Mole challenge. hit = true presses every target correctly.
    private static void playWhack(GameEngine engine, boolean hit) {
        WhackAMole whack = (WhackAMole) engine.getGame();
        while (engine.getGame() == whack) {
            wait(engine, 10);
            if (whack.isTargetLit() && hit) {
                press(engine, whack.getTarget());
            }
        }
    }

    private static void cardPicksGame() {
        GameEngine engine = newEngine();
        startGame(engine, "BBBBBBBBBB");
        check("card B starts Memory", engine.getGame() instanceof MemoryGame);
        check("state is PLAYING", engine.getState() == GameEngine.PLAYING);
    }

    private static void unknownCard() {
        GameEngine engine = newEngine();
        startGame(engine, "1234567890");
        check("unknown card does not start a game", engine.getState() == GameEngine.READY);
    }

    private static void whackPassMovesPotato() {
        GameEngine engine = newEngine();
        startGame(engine, "AAAAAAAAAA");
        playWhack(engine, true);
        check("passing gives the potato to P2", engine.getHolder() == 1);
        check("passing keeps 3 fuses", engine.getFuses() == 3);
    }

    private static void memoryMistakeCostsFuse() {
        GameEngine engine = newEngine();
        startGame(engine, "BBBBBBBBBB");
        MemoryGame memory = (MemoryGame) engine.getGame();
        while (!memory.isAnswering()) {
            wait(engine, 10);
        }
        int wrong = (memory.getPattern()[0] + 1) % 4;
        press(engine, wrong);
        wait(engine, 20);
        check("a wrong color costs one fuse", engine.getFuses() == 2);
        check("P1 keeps the potato", engine.getHolder() == 0);
    }

    private static void threeFailsLoseRound() {
        GameEngine engine = newEngine();
        for (int i = 0; i < 3; i++) {
            startGame(engine, "AAAAAAAAAA");
            playWhack(engine, false);
        }
        check("P2 wins the round after 3 fails", engine.getWins(1) == 1);
        wait(engine, 3100);
        check("round 2 starts", engine.getRound() == 2 && engine.getState() == GameEngine.READY);
        check("P2 starts round 2", engine.getHolder() == 1);
    }

    private static void duelWinnerTakesRound() {
        GameEngine engine = newEngine();
        startGame(engine, "CCCCCCCCCC");
        LightDuel duel = (LightDuel) engine.getGame();
        while (engine.getGame() == duel) {
            wait(engine, 10);
            if (duel.isCueShowing()) {
                // Player 1: button 1 (green) is left, button 0 (blue) is right
                press(engine, duel.getCueSide() == 0 ? 1 : 0);
            }
        }
        check("P1 wins the round by winning the duel", engine.getWins(0) == 1);
    }

    private static void liftingHandTwiceLosesRound() {
        GameEngine engine = newEngine();
        engine.setUseSensor(true);
        engine.setCoveredLimit(50);
        engine.receive("LIGHT 10", now);
        startGame(engine, "AAAAAAAAAA");
        engine.receive("LIGHT 500", now);   // hand lifted: warning
        wait(engine, 500);
        engine.receive("LIGHT 10", now);    // covered again in time
        wait(engine, 500);
        check("first lift only warns", engine.getState() == GameEngine.PLAYING);
        engine.receive("LIGHT 500", now);   // lifted a second time
        check("second lift loses the round", engine.getWins(1) == 1);
    }

    private static void coverSensorToStart() {
        GameEngine engine = newEngine();
        engine.setUseSensor(true);
        engine.setCoveredLimit(50);
        engine.receive("LIGHT 500", now);
        startGame(engine, "AAAAAAAAAA");
        check("cannot start Whack-a-Mole uncovered", engine.getState() == GameEngine.READY);
        engine.receive("LIGHT 10", now);
        press(engine, 0);
        check("starts once covered", engine.getState() == GameEngine.PLAYING);
    }

    private static void timerRunsOut() {
        GameEngine engine = newEngine();
        startGame(engine, "AAAAAAAAAA");
        playWhack(engine, false);   // fail once, then wait without playing
        wait(engine, 95000);
        check("the hidden timer ends the round", engine.getWins(1) == 1);
    }

    private static void hardModeChangesRules() {
        GameEngine engine = newEngine();
        engine.setDifficulty(new HardDifficulty());
        startGame(engine, "BBBBBBBBBB");
        MemoryGame memory = (MemoryGame) engine.getGame();
        check("Hard Memory starts with 5 colors", memory.getPattern().length == 5);
    }
}
