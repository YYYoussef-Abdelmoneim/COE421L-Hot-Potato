// Starts the Hot Potato Arcade program.
public class Main {
    public static void main(String[] args) {
        SerialLink gameLink = new SerialLink();   // main LilyPad, through the XBee radio
        SerialLink lampLink = new SerialLink();   // second LilyPad, through USB
        Hardware hardware = new Hardware(gameLink, lampLink);

        CardList cards = new CardList("rfid-cards.txt");
        LightCalibration calibration = new LightCalibration("light-calibration.txt");

        GameEngine engine = new GameEngine(hardware, cards);
        engine.setCoveredLimit(calibration.getLimit());

        // Observer pattern: the engine gets every message from the main board
        gameLink.registerObserver(engine);

        GameWindow window = new GameWindow(engine, gameLink, lampLink);

        // Debug screen. To remove it, delete this line and the file DebugPanel.java.
        window.addTab("Debug", new DebugPanel(engine, gameLink, lampLink, cards, calibration));

        window.setVisible(true);
    }
}
