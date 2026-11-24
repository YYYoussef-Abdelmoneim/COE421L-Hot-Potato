// Turns game actions into the text commands that the two boards understand.
public class Hardware {
    private SerialLink gameLink;   // main LilyPad, through the XBee radio
    private SerialLink lampLink;   // second LilyPad, through USB

    // The last state we sent, so we can send it again
    private int lamps = 0;
    private int side = 0;
    private boolean readerOn = false;
    private String top = "";
    private String bottom = "";

    public Hardware(SerialLink gameLink, SerialLink lampLink) {
        this.gameLink = gameLink;
        this.lampLink = lampLink;
    }

    // Button lamps. Each bit is one lamp: 1 blue, 2 green, 4 red, 8 yellow.
    public void setLamps(int mask) {
        lamps = mask;
        lampLink.send(Integer.toHexString(mask).toUpperCase());
    }

    // Red side lights: 0 = off, 1 = left (Player 1), 2 = right (Player 2).
    public void setSide(int newSide) {
        if (newSide != side) {
            side = newSide;
            gameLink.send("SIDE " + side);
        }
    }

    // Turns the RFID card reader on or off.
    public void setReader(boolean on) {
        if (on != readerOn) {
            readerOn = on;
            gameLink.send(on ? "RFID ON" : "RFID OFF");
        }
    }

    // Sound names: START, HIT, MISS, WARN, PASS, DROP, TICK, OFF.
    public void playSound(String name) {
        gameLink.send("SOUND " + name);
    }

    // Writes two lines on the LCD. Only lines that changed are sent.
    public void showLcd(String newTop, String newBottom) {
        newTop = fit(newTop);
        newBottom = fit(newBottom);
        if (!newTop.equals(top)) {
            top = newTop;
            gameLink.send("LCD 0 " + top);
        }
        if (!newBottom.equals(bottom)) {
            bottom = newBottom;
            gameLink.send("LCD 1 " + bottom);
        }
    }

    // Called once a second. Sends everything again in case a radio message was lost.
    // This also tells both boards that the laptop is still there.
    public void refresh() {
        gameLink.send("SIDE " + side);
        gameLink.send(readerOn ? "RFID ON" : "RFID OFF");
        gameLink.send("LCD 0 " + top);
        gameLink.send("LCD 1 " + bottom);
        lampLink.send("?");
        lampLink.send(Integer.toHexString(lamps).toUpperCase());
    }

    // Used in test mode: only keeps both boards awake.
    public void keepAlive() {
        gameLink.send("PING");
        lampLink.send("?");
    }

    public String getTop() {
        return top;
    }

    public String getBottom() {
        return bottom;
    }

    // The LCD has 16 characters per line.
    private String fit(String text) {
        if (text.length() > 16) {
            return text.substring(0, 16);
        }
        return text;
    }
}
