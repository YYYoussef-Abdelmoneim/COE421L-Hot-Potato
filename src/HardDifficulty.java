// Strategy pattern: the Hard difficulty.
public class HardDifficulty implements DifficultyStrategy {
    public String getName() {
        return "Hard";
    }

    // Starts at 0.75 s and gets 0.1 s faster each challenge, down to 0.35 s.
    public long whackTime(int challengesDone) {
        return Math.max(350, 750 - 100 * Math.min(6, challengesDone));
    }

    public int whackHitsNeeded() {
        return 7;
    }

    public int memoryStartColors() {
        return 5;
    }

    public long memoryAnswerTime(int colors) {
        return 1000 + 850 * colors;
    }

    public long memoryFlashTime() {
        return 450;
    }

    public long duelWindow() {
        return 1000;
    }

    public int duelPointsToWin() {
        return 5;
    }

    public boolean duelPenalty() {
        return true;
    }
}
