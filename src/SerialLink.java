import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;

// One serial connection to one board.
// It reads messages on its own thread and sends each full line to its observers.
public class SerialLink implements Subject, Runnable {
    private SerialPort port;
    private boolean running = false;
    private ArrayList<Observer> observers = new ArrayList<Observer>();

    // Opens the port at 9600 baud and starts the reading thread.
    public boolean open(String portPath) {
        port = SerialPort.getCommPort(portPath);
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        if (!port.openPort()) {
            port = null;
            return false;
        }
        running = true;
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public void close() {
        running = false;
        if (port != null) {
            port.closePort();
        }
        port = null;
    }

    public boolean isOpen() {
        return port != null;
    }

    // Sends one line to the board.
    public synchronized void send(String text) {
        if (port == null) {
            return;
        }
        byte[] bytes = (text + "\n").getBytes();
        port.writeBytes(bytes, bytes.length);
        try {
            Thread.sleep(5); // a short gap so the radio is not flooded
        } catch (InterruptedException e) {
            // nothing to do
        }
    }

    // The reading thread. It collects characters until a line ends.
    public void run() {
        SerialPort myPort = port;
        StringBuilder line = new StringBuilder();
        byte[] buffer = new byte[64];
        while (running) {
            int count = myPort.readBytes(buffer, buffer.length);
            if (count < 0) {
                notifyObservers("LINK LOST");
                running = false;
            }
            for (int i = 0; i < count; i++) {
                char c = (char) buffer[i];
                if (c == '\n' || c == '\r') {
                    if (line.length() > 0) {
                        notifyObservers(line.toString());
                        line.setLength(0);
                    }
                } else if (line.length() < 100) {
                    line.append(c);
                }
            }
        }
    }

    public void registerObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    public void notifyObservers(String message) {
        for (int i = 0; i < observers.size(); i++) {
            observers.get(i).update(message);
        }
    }
}
