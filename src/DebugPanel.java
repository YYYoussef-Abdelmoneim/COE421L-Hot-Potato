import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// Debug screen for testing each part of the box.
// To remove it, delete this file and its line in Main.java.
// Observer pattern: it is an observer of both boards, so it can show every message.
public class DebugPanel extends JPanel implements Observer {
    private GameEngine engine;
    private SerialLink gameLink;
    private SerialLink lampLink;
    private CardList cards;
    private LightCalibration calibration;

    private JTextArea log = new JTextArea();
    private JCheckBox testMode = new JCheckBox("Test mode (pauses the game)");
    private JCheckBox showLight = new JCheckBox("Show LIGHT messages");
    private JLabel lightLabel = new JLabel("Light: -");
    private JLabel cardLabel = new JLabel();
    private int lastLight = -1;
    private int coveredReading;
    private int openReading;
    private int cardsToLearn = 0;   // how many new cards we are still waiting for

    public DebugPanel(GameEngine engine, SerialLink gameLink, SerialLink lampLink,
                      CardList cards, LightCalibration calibration) {
        this.engine = engine;
        this.gameLink = gameLink;
        this.lampLink = lampLink;
        this.cards = cards;
        this.calibration = calibration;
        coveredReading = calibration.getCovered();
        openReading = calibration.getOpen();

        // Observer pattern: listen to both boards
        gameLink.registerObserver(this);
        lampLink.registerObserver(this);

        JPanel controls = new JPanel(new GridLayout(7, 1));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(testMode);
        row1.add(showLight);
        row1.add(button("Ping boards", "PING", "?"));
        row1.add(clearButton());
        controls.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Lamps:"));
        row2.add(button("Blue", null, "1"));
        row2.add(button("Green", null, "2"));
        row2.add(button("Red", null, "4"));
        row2.add(button("Yellow", null, "8"));
        row2.add(button("All", null, "F"));
        row2.add(button("Off", null, "0"));
        controls.add(row2);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("Side lights:"));
        row3.add(button("Left", "SIDE 1", null));
        row3.add(button("Right", "SIDE 2", null));
        row3.add(button("Off", "SIDE 0", null));
        row3.add(new JLabel("   LCD:"));
        row3.add(button("Test text", "LCD 0 LCD TEST LINE1", null));
        row3.add(button("Line 2", "LCD 1 0123456789ABCDEF", null));
        controls.add(row3);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row4.add(new JLabel("Sounds:"));
        String[] sounds = {"START", "HIT", "MISS", "WARN", "PASS", "DROP", "TICK"};
        for (int i = 0; i < sounds.length; i++) {
            row4.add(button(sounds[i], "SOUND " + sounds[i], null));
        }
        controls.add(row4);

        JPanel row5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row5.add(new JLabel("Card reader:"));
        row5.add(button("On", "RFID ON", null));
        row5.add(button("Off", "RFID OFF", null));
        JButton learnButton = new JButton("Learn 3 new cards");
        learnButton.addActionListener(e -> {
            cardsToLearn = 3;
            gameLink.send("RFID ON");
            addLine("Scan the cards in order: Whack-a-Mole, Memory, Light Duel");
        });
        row5.add(learnButton);
        controls.add(row5);
        controls.add(cardLabel);

        JPanel row6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row6.add(lightLabel);
        JButton coveredButton = new JButton("Save as covered");
        coveredButton.addActionListener(e -> {
            coveredReading = lastLight;
            saveCalibration();
        });
        JButton openButton = new JButton("Save as open");
        openButton.addActionListener(e -> {
            openReading = lastLight;
            saveCalibration();
        });
        row6.add(coveredButton);
        row6.add(openButton);
        controls.add(row6);

        testMode.addActionListener(e -> engine.setPaused(testMode.isSelected()));

        log.setEditable(false);
        setLayout(new BorderLayout());
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);
        showCards();
    }

    // Makes a button that sends a command to the main board and/or the lamp board.
    private JButton button(String text, String gameCommand, String lampCommand) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            if (!testMode.isSelected()) {
                addLine("Turn on test mode first, or the game will undo this.");
            }
            if (gameCommand != null) {
                gameLink.send(gameCommand);
                addLine("> " + gameCommand);
            }
            if (lampCommand != null) {
                lampLink.send(lampCommand);
                addLine("> lamps " + lampCommand);
            }
        });
        return b;
    }

    private JButton clearButton() {
        JButton b = new JButton("Clear log");
        b.addActionListener(e -> log.setText(""));
        return b;
    }

    // Observer pattern: called for every message from either board.
    // It runs on the serial thread, so we hand the work to the Swing thread.
    public void update(String message) {
        SwingUtilities.invokeLater(() -> handle(message));
    }

    private void handle(String message) {
        if (message.startsWith("LIGHT ")) {
            try {
                lastLight = Integer.parseInt(message.substring(6));
            } catch (NumberFormatException e) {
                return;
            }
            lightLabel.setText("Light: " + lastLight + "   covered = " + coveredReading
                    + "   open = " + openReading + "   limit = " + calibration.getLimit());
            if (!showLight.isSelected()) {
                return;
            }
        }
        if (message.startsWith("CARD ") && cardsToLearn > 0) {
            learnCard(message.substring(5));
        }
        addLine(message);
    }

    // Saves the next three different cards as A (Whack-a-Mole), B (Memory) and C (Light Duel).
    private void learnCard(String id) {
        int slot = 3 - cardsToLearn;
        for (int i = 0; i < slot; i++) {
            if (cards.getCard(i).equals(id)) {
                return; // this card was already used for an earlier game
            }
        }
        cards.setCard(slot, id);
        cardsToLearn--;
        addLine("Saved " + id + " for " + GameEngine.GAME_NAMES[slot]);
        if (cardsToLearn == 0) {
            cards.save();
            addLine("All three cards saved to rfid-cards.txt");
        }
        showCards();
    }

    private void saveCalibration() {
        calibration.save(coveredReading, openReading);
        engine.setCoveredLimit(calibration.getLimit());
        addLine("Light saved: covered " + coveredReading + ", open " + openReading
                + ", limit " + calibration.getLimit());
    }

    private void showCards() {
        cardLabel.setText("  Cards:  A Whack-a-Mole = " + cards.getCard(0) + "   B Memory = "
                + cards.getCard(1) + "   C Light Duel = " + cards.getCard(2));
    }

    private void addLine(String text) {
        log.append(text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
