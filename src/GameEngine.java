import java.util.Random;

// The brain of the game. It follows the rules, keeps the score and tells the hardware what to do.
// Observer pattern: it is an observer of the main board, so it gets every message the box sends.
// Strategy pattern: it is the context. It uses whatever difficulty is set.
public class GameEngine implements Observer {
    // Game states
    public static final int IDLE = 0;         // no match yet
    public static final int READY = 1;        // waiting for a card and a button press
    public static final int PLAYING = 2;      // a mini-game is running
    public static final int ROUND_OVER = 3;   // short pause before the next round
    public static final int MATCH_OVER = 4;   // someone won three rounds

    public static final String[] GAME_NAMES = {"Whack-a-Mole", "Memory", "Light Duel"};

    private Hardware hardware;
    private CardList cards;
    private DifficultyStrategy difficulty = new NormalDifficulty();
    private PotatoTimer potatoTimer = new PotatoTimer();
    private Random random = new Random();

    private int state = IDLE;
    private int holder;            // player with the potato: 0 = P1, 1 = P2
    private int round;
    private int fuses;
    private int[] wins = new int[2];
    private int roundWinner;
    private int challengesDone;    // makes the games harder as the round goes on
    private int chosenGame = -1;   // game picked with a card, -1 = none
    private MiniGame game;         // the mini-game being played
    private String note = "";      // short message for the LCD, like "Fuse lost!"

    private boolean[] held = new boolean[4];   // which buttons are down right now
    private int light = -1;                    // last light sensor reading
    private int coveredLimit = 50;             // a reading at or below this means covered
    private boolean useSensor = true;          // the one-hand rule is on
    private boolean warningUsed;               // the holder already lifted their hand once
    private long warningEnds = 0;              // 0 = no warning running

    private boolean timerStarted;
    private long graceEnds;     // after a pass, the timer waits 5 seconds
    private long nextTick;      // when to play the next tick sound
    private long nextRefresh;   // when to send the hardware state again
    private long stateEnds;     // when ROUND_OVER or MATCH_OVER can end
    private boolean paused;     // true while the debug screen is testing parts

    public GameEngine(Hardware hardware, CardList cards) {
        this.hardware = hardware;
        this.cards = cards;
    }

    // Starts a new match: scores go back to 0-0.
    public synchronized void newMatch(long now) {
        wins[0] = 0;
        wins[1] = 0;
        round = 1;
        startRound(now);
    }

    private void startRound(long now) {
        holder = (round - 1) % 2;   // players take turns starting a round
        fuses = 3;
        challengesDone = 0;
        chosenGame = -1;
        game = null;
        note = "";
        potatoTimer.reset(60000 + random.nextInt(30001)); // hidden: 60 to 90 seconds
        timerStarted = false;
        graceEnds = 0;
        nextTick = now;
        hardware.setLamps(0);
        hardware.playSound("OFF");
        state = READY;
    }

    // Observer pattern: SerialLink calls this for every line the main board sends.
    public void update(String message) {
        receive(message, System.currentTimeMillis());
    }

    public synchronized void receive(String message, long now) {
        if (paused) {
            return;
        }
        String[] parts = message.split(" ");
        try {
            if (parts[0].equals("BTN") && parts.length == 3) {
                int button = Integer.parseInt(parts[1]) - 1;
                boolean down = parts[2].equals("DOWN");
                boolean freshPress = down && !held[button];
                held[button] = down;
                if (freshPress) {
                    buttonPressed(button, now);
                }
            } else if (parts[0].equals("CARD") && parts.length == 2) {
                cardScanned(parts[1]);
            } else if (parts[0].equals("LIGHT") && parts.length == 2) {
                lightReading(Integer.parseInt(parts[1]), now);
            }
        } catch (RuntimeException e) {
            // A broken radio message. Ignore it.
        }
    }

    private void cardScanned(String id) {
        if (state != READY) {
            return; // cards only work between challenges
        }
        int g = cards.gameFor(id);
        if (g < 0) {
            note = "Unknown card";
            chosenGame = -1;
        } else {
            note = "";
            chosenGame = g;
        }
    }

    private void buttonPressed(int button, long now) {
        if (state == READY && chosenGame >= 0) {
            startChallenge(now);
        } else if (state == PLAYING) {
            game.pressed(button, now);
        } else if (state == MATCH_OVER && now >= stateEnds) {
            newMatch(now);
        }
    }

    private void startChallenge(long now) {
        MiniGame next = makeGame(chosenGame);
        if (sensorRuleFor(next) && !isCovered()) {
            note = "Cover sensor!";
            return;
        }
        game = next;
        chosenGame = -1;
        note = "";
        warningUsed = false;
        warningEnds = 0;
        timerStarted = true;
        hardware.playSound("START");
        state = PLAYING;
        game.start(now);
    }

    // Polymorphism: the engine treats all three games as a MiniGame.
    private MiniGame makeGame(int number) {
        if (number == 0) {
            return new WhackAMole(this);
        } else if (number == 1) {
            return new MemoryGame(this);
        }
        return new LightDuel(this);
    }

    // One-hand rule: in one-player games the holder must keep the light sensor covered.
    // The first time they lift their hand they get 2 seconds to cover it again.
    // Lifting it a second time, or running out of time, loses the round.
    private void lightReading(int value, long now) {
        light = value;
        if (state != PLAYING || !sensorRuleFor(game)) {
            return;
        }
        if (isCovered()) {
            warningEnds = 0;
        } else if (warningEnds == 0) {
            if (warningUsed) {
                dropPotato("Hand lifted", now);
            } else {
                warningUsed = true;
                warningEnds = now + 2000;
                hardware.playSound("WARN");
            }
        }
    }

    // Called about 20 times a second by the window's timer.
    public synchronized void tick(long now) {
        if (now >= nextRefresh) {
            nextRefresh = now + 1000;
            if (paused) {
                hardware.keepAlive();
            } else {
                hardware.refresh();
            }
        }
        if (state == IDLE) {
            hardware.showLcd(lcdTop(), lcdBottom());
            return;
        }
        if (paused) {
            return;
        }
        if (state == ROUND_OVER && now >= stateEnds) {
            round++;
            startRound(now);
        }
        if (state == READY || state == PLAYING) {
            updateTimer(now);
            if (potatoTimer.isExpired(now)) {
                dropPotato("Time ran out", now);
            } else if (warningEnds > 0 && now >= warningEnds) {
                dropPotato("Hand lifted", now);
            } else {
                if (now >= nextTick) {
                    hardware.playSound("TICK");
                    nextTick = now + potatoTimer.tickGap(now);
                }
                if (state == PLAYING) {
                    game.tick(now);
                    if (game.isFinished()) {
                        challengeOver(now);
                    }
                }
            }
        }
        hardware.setReader(state == READY);
        if (!(state == PLAYING && game.isTwoPlayer())) {
            hardware.setSide(holder + 1); // Light Duel controls the side lights itself
        }
        hardware.showLcd(lcdTop(), lcdBottom());
    }

    // The hidden timer starts with the first challenge of the round.
    // It stops during Light Duel, while the holder covers the sensor, and for 5 seconds after a pass.
    private void updateTimer(long now) {
        boolean duel = state == PLAYING && game.isTwoPlayer();
        boolean covering = useSensor && isCovered();
        if (timerStarted && !duel && !covering && now >= graceEnds) {
            potatoTimer.resume(now);
        } else {
            potatoTimer.pause(now);
        }
    }

    private void challengeOver(long now) {
        hardware.setLamps(0);
        if (game.isTwoPlayer()) {
            hardware.playSound("DROP");
            note = "P" + (game.getWinner() + 1) + " won the duel";
            roundWon(game.getWinner(), now);
            return;
        }
        challengesDone++;
        if (game.isPassed()) {
            holder = 1 - holder;   // pass the potato
            graceEnds = now + 5000;
            note = "Potato passed!";
            hardware.playSound("PASS");
            state = READY;
        } else {
            fuses--;
            if (fuses == 0) {
                hardware.playSound("DROP");
                note = "No fuses left";
                roundWon(1 - holder, now);
            } else {
                note = "Fuse lost!";
                hardware.playSound("MISS");
                state = READY;
            }
        }
        game = null;
    }

    // The holder loses the round right away.
    private void dropPotato(String reason, long now) {
        hardware.setLamps(0);
        hardware.playSound("DROP");
        note = reason;
        game = null;
        roundWon(1 - holder, now);
    }

    private void roundWon(int winner, long now) {
        wins[winner]++;
        roundWinner = winner;
        potatoTimer.pause(now);
        if (wins[winner] >= 3) {
            state = MATCH_OVER;
            stateEnds = now + 2000;
        } else {
            state = ROUND_OVER;
            stateEnds = now + 3000;
        }
    }

    private String lcdTop() {
        String p = "P" + (holder + 1);
        if (state == READY) {
            if (chosenGame >= 0) {
                return p + " " + GAME_NAMES[chosenGame];
            }
            return p + " scan a card";
        } else if (state == PLAYING) {
            return game.lcdTop();
        } else if (state == ROUND_OVER) {
            return "P" + (roundWinner + 1) + " wins round";
        } else if (state == MATCH_OVER) {
            return "P" + (roundWinner + 1) + " wins match!";
        }
        return "Hot Potato";
    }

    private String lcdBottom() {
        if (state == READY) {
            if (!note.isEmpty()) {
                return note;
            }
            if (chosenGame >= 0) {
                return (chosenGame != 2 && useSensor) ? "Cover + press" : "Press to start";
            }
            return "P1:" + wins[0] + " P2:" + wins[1] + " Fuse " + fuses;
        } else if (state == PLAYING) {
            if (warningEnds > 0) {
                return "Cover sensor!";
            }
            return game.lcdBottom();
        } else if (state == ROUND_OVER) {
            return note;
        } else if (state == MATCH_OVER) {
            return "Press to replay";
        }
        return "Connect laptop";
    }

    public synchronized String getStatus() {
        String gameName = (game == null) ? "-" : game.getClass().getSimpleName();
        return "Round " + round + "   P1 " + wins[0] + " - " + wins[1] + " P2   Fuses " + fuses
                + "   Game " + gameName + "   Light " + light + " (covered at " + coveredLimit + " or less)"
                + "   Timer " + (potatoTimer.isRunning() ? "running" : "paused");
    }

    // Used by the mini-games
    public boolean anyButtonHeld() {
        return held[0] || held[1] || held[2] || held[3];
    }

    private boolean sensorRuleFor(MiniGame g) {
        return useSensor && !g.isTwoPlayer();
    }

    private boolean isCovered() {
        return light >= 0 && light <= coveredLimit;
    }

    // Strategy pattern: change the difficulty while the program runs.
    public synchronized void setDifficulty(DifficultyStrategy newDifficulty) {
        difficulty = newDifficulty;
    }

    public synchronized void setUseSensor(boolean on) {
        useSensor = on;
    }

    public synchronized void setCoveredLimit(int limit) {
        coveredLimit = limit;
    }

    public synchronized void setPaused(boolean on) {
        paused = on;
        potatoTimer.pause(System.currentTimeMillis());
    }

    public Hardware getHardware() {
        return hardware;
    }

    public DifficultyStrategy getDifficulty() {
        return difficulty;
    }

    public Random getRandom() {
        return random;
    }

    public int getHolder() {
        return holder;
    }

    public int getChallengesDone() {
        return challengesDone;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized int getFuses() {
        return fuses;
    }

    public synchronized int getWins(int player) {
        return wins[player];
    }

    public synchronized int getRound() {
        return round;
    }

    public synchronized MiniGame getGame() {
        return game;
    }

    public synchronized int getLight() {
        return light;
    }
}
