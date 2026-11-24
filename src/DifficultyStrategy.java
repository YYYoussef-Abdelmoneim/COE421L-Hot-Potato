// Strategy pattern: each difficulty answers the same questions in its own way.
// The game engine asks these questions without knowing which difficulty is set.
public interface DifficultyStrategy {
    String getName();

    // Whack-a-Mole
    long whackTime(int challengesDone);   // time to hit one target (ms)
    int whackHitsNeeded();                // hits out of 8 targets needed to pass

    // Memory
    int memoryStartColors();              // colors in the first pattern
    long memoryAnswerTime(int colors);    // time to repeat the whole pattern (ms)
    long memoryFlashTime();               // how long each color is shown (ms)

    // Light Duel
    long duelWindow();                    // time to react after the cue (ms)
    int duelPointsToWin();
    boolean duelPenalty();                // true = an early or wrong press gives the other player a point
}
