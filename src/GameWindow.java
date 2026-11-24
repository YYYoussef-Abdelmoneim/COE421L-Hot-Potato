import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

// The main window: pick the two ports, connect, and watch the game.
public class GameWindow extends JFrame {
    private GameEngine engine;
    private SerialLink gameLink;
    private SerialLink lampLink;

    private JComboBox<String> gamePortBox = new JComboBox<String>();
    private JComboBox<String> lampPortBox = new JComboBox<String>();
    private JButton connectButton = new JButton("Connect");
    private JComboBox<String> difficultyBox = new JComboBox<String>(new String[] {"Normal", "Hard"});
    private JCheckBox sensorBox = new JCheckBox("One-hand rule (light sensor)", true);
    private JLabel lcdTop = new JLabel(" ");
    private JLabel lcdBottom = new JLabel(" ");
    private JLabel status = new JLabel(" ");
    private JTabbedPane tabs = new JTabbedPane();

    public GameWindow(GameEngine engine, SerialLink gameLink, SerialLink lampLink) {
        super("Hot Potato Arcade");
        this.engine = engine;
        this.gameLink = gameLink;
        this.lampLink = lampLink;

        // Row 1: ports
        JButton refreshButton = new JButton("Refresh");
        JPanel portRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portRow.add(new JLabel("Game (XBee):"));
        portRow.add(gamePortBox);
        portRow.add(new JLabel("Lamps (USB):"));
        portRow.add(lampPortBox);
        portRow.add(refreshButton);
        portRow.add(connectButton);

        // Row 2: game options
        JButton newMatchButton = new JButton("New match");
        JPanel optionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionRow.add(new JLabel("Difficulty:"));
        optionRow.add(difficultyBox);
        optionRow.add(sensorBox);
        optionRow.add(newMatchButton);

        JPanel topRows = new JPanel(new GridLayout(2, 1));
        topRows.add(portRow);
        topRows.add(optionRow);

        // A copy of the box's LCD, plus the score
        Font lcdFont = new Font(Font.MONOSPACED, Font.BOLD, 30);
        lcdTop.setFont(lcdFont);
        lcdBottom.setFont(lcdFont);
        JPanel lcdPanel = new JPanel(new GridLayout(3, 1));
        lcdPanel.setBackground(Color.BLACK);
        lcdTop.setForeground(Color.GREEN);
        lcdBottom.setForeground(Color.GREEN);
        status.setForeground(Color.WHITE);
        lcdPanel.add(lcdTop);
        lcdPanel.add(lcdBottom);
        lcdPanel.add(status);

        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(topRows, BorderLayout.NORTH);
        gamePanel.add(lcdPanel, BorderLayout.CENTER);
        tabs.addTab("Game", gamePanel);
        add(tabs);

        // What each button does
        refreshButton.addActionListener(e -> listPorts());
        connectButton.addActionListener(e -> connectOrDisconnect());
        newMatchButton.addActionListener(e -> engine.newMatch(System.currentTimeMillis()));
        sensorBox.addActionListener(e -> engine.setUseSensor(sensorBox.isSelected()));
        difficultyBox.addActionListener(e -> {
            // Strategy pattern: swap the difficulty object
            if (difficultyBox.getSelectedIndex() == 0) {
                engine.setDifficulty(new NormalDifficulty());
            } else {
                engine.setDifficulty(new HardDifficulty());
            }
        });

        // Every 50 ms: move the game forward and update the screen
        Timer timer = new Timer(50, e -> {
            engine.tick(System.currentTimeMillis());
            showState();
        });
        timer.start();

        listPorts();
        setSize(900, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Lets Main add more tabs, like the debug screen.
    public void addTab(String title, JPanel panel) {
        tabs.addTab(title, panel);
    }

    private void listPorts() {
        gamePortBox.removeAllItems();
        lampPortBox.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (int i = 0; i < ports.length; i++) {
            gamePortBox.addItem(ports[i].getSystemPortPath());
            lampPortBox.addItem(ports[i].getSystemPortPath());
        }
    }

    private void connectOrDisconnect() {
        if (gameLink.isOpen()) {
            gameLink.close();
            lampLink.close();
            connectButton.setText("Connect");
            return;
        }
        String gamePort = (String) gamePortBox.getSelectedItem();
        String lampPort = (String) lampPortBox.getSelectedItem();
        if (gamePort == null || gamePort.equals(lampPort)) {
            JOptionPane.showMessageDialog(this, "Pick two different ports.");
            return;
        }
        if (!gameLink.open(gamePort)) {
            JOptionPane.showMessageDialog(this, "Could not open the game port " + gamePort);
            return;
        }
        if (!lampLink.open(lampPort)) {
            JOptionPane.showMessageDialog(this, "Could not open the lamp port " + lampPort + ". The lamps will not work.");
        }
        connectButton.setText("Disconnect");
        engine.newMatch(System.currentTimeMillis());
    }

    private void showState() {
        lcdTop.setText(" " + engine.getHardware().getTop());
        lcdBottom.setText(" " + engine.getHardware().getBottom());
        status.setText(" " + engine.getStatus());
    }
}
