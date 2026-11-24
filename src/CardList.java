import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

// The three RFID card IDs, saved in a text file.
// Line 1 = Whack-a-Mole, line 2 = Memory, line 3 = Light Duel.
public class CardList {
    private String fileName;
    private String[] ids = {"", "", ""};

    public CardList(String fileName) {
        this.fileName = fileName;
        load();
    }

    public void load() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            for (int i = 0; i < 3; i++) {
                String line = in.readLine();
                if (line != null) {
                    ids[i] = line.trim();
                }
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Could not read " + fileName);
        }
    }

    public void save() {
        try {
            PrintWriter out = new PrintWriter(fileName);
            for (int i = 0; i < 3; i++) {
                out.println(ids[i]);
            }
            out.close();
        } catch (IOException e) {
            System.out.println("Could not save " + fileName);
        }
    }

    // Returns 0, 1 or 2 for the game on this card, or -1 if the card is unknown.
    public int gameFor(String id) {
        for (int i = 0; i < 3; i++) {
            if (ids[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public void setCard(int game, String id) {
        ids[game] = id;
    }

    public String getCard(int game) {
        return ids[game];
    }
}
