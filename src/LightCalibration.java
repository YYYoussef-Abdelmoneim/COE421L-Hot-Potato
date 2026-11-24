import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

// Light sensor readings for "covered" and "open", saved in a text file.
// The file has one line with two numbers, for example: 821 874
public class LightCalibration {
    private String fileName;
    private int covered = 0;
    private int open = 100;

    public LightCalibration(String fileName) {
        this.fileName = fileName;
        load();
    }

    public void load() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String[] parts = in.readLine().trim().split(" ");
            covered = Integer.parseInt(parts[0]);
            open = Integer.parseInt(parts[1]);
            in.close();
        } catch (IOException | RuntimeException e) {
            System.out.println("Could not read " + fileName + ", using default values");
        }
    }

    public void save(int newCovered, int newOpen) {
        covered = newCovered;
        open = newOpen;
        try {
            PrintWriter out = new PrintWriter(fileName);
            out.println(covered + " " + open);
            out.close();
        } catch (IOException e) {
            System.out.println("Could not save " + fileName);
        }
    }

    // A reading at or below this number means the sensor is covered.
    // It is halfway between the covered and open readings.
    public int getLimit() {
        return (covered + open) / 2;
    }

    public int getCovered() {
        return covered;
    }

    public int getOpen() {
        return open;
    }
}
