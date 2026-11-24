# Hot Potato Arcade

A two-player game in one box. A Java program on the laptop runs the game. It talks to two LilyPad Arduino boards: the main board over an XBee radio, and the lamp board over USB.

## Folders

- `src` has the Java program.
- `arduino/MainArcade_Match` has the main board code. Open `MainArcade_Match.ino`; the other files open as tabs.
- `arduino/Button_Lamps` has the lamp board code.
- `lib` has the serial library (jSerialComm).
- `rfid-cards.txt` has the three card IDs: line 1 Whack-a-Mole, line 2 Memory, line 3 Light Duel.
- `light-calibration.txt` has the light sensor readings for covered and open.

Keep the two `.txt` files in the folder you run the program from.

## Run in Eclipse

1. File > New > Java Project. Untick "Create module-info.java".
2. Copy all files from `src` into the project's `src` folder.
3. Right-click the project > Build Path > Add External Archives, and pick `lib/jSerialComm-2.11.4.jar`.
4. Copy `rfid-cards.txt` and `light-calibration.txt` into the project folder (next to `src`, not inside it).
5. Run `Main.java`.

## Run from a terminal

macOS: run `Start_Mac.command`.

Windows (inside this folder):

```
mkdir classes
javac -cp "lib/*" -d classes src/*.java
java -cp "classes;lib/*" Main
```

To run the software tests, use `GameTests` instead of `Main`.

## Upload the Arduino code

Use Arduino IDE with the board set to LilyPad Arduino, processor ATmega328. Install the hd44780 library (Sketch > Include Library > Manage Libraries). Upload `MainArcade_Match` to the main board and `Button_Lamps` to the lamp board. Unplug the XBee data wires (D0 and D1) while uploading to the main board, then plug them back in.

## Remove the debug screen

Delete `DebugPanel.java` and the line in `Main.java` that adds the Debug tab.
