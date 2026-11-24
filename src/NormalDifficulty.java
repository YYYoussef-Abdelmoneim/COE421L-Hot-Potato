// Strategy pattern: the Normal difficulty.
public class NormalDifficulty implements DifficultyStrategy {
    public String getName() {
        return "Normal";
    }

    // Starts at 1.0 s and gets 0.1 s faster each challenge, down to 0.4 s.
    public long whackTime(int challengesDone) {
        return Math.max(400, 1000 - 100 * Math.min(6, challengesDone));
    }

    public int whackHitsNeeded() {
        return 6;
    }

    public int memoryStartColors() {
        return 3;
    }

    public long memoryAnswerTime(int colors) {
        return 2000 + 1500 * colors;
    }

    public long memoryFlashTime() {
        return 650;
    }

    public long duelWindow() {
        return 1500;
    }

    public int duelPointsToWin() {
        return 3;
    }

    public boolean duelPenalty() {
        return false;
    }
}
