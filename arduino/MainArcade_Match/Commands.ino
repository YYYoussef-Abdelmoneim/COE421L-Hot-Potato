// Reads commands from Java, one line at a time, and carries them out.
//
// Commands:
//   PING          replies PONG
//   SIDE n        red side lights: 0 = off, 1 = left, 2 = right
//   LCD r text    writes text on LCD row r (0 = top, 1 = bottom)
//   SOUND name    START, HIT, MISS, WARN, PASS, DROP, TICK or OFF
//   RFID ON       turns the card reader on
//   RFID OFF      turns the card reader off

char command[40];
byte commandLength = 0;

void readCommands() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    if (c == '\n' || c == '\r') {
      if (commandLength > 0) {
        command[commandLength] = '\0';
        runCommand();
        commandLength = 0;
      }
    } else if (commandLength < 39) {
      command[commandLength] = c;
      commandLength++;
    }
  }
}

void runCommand() {
  lastCommandTime = millis();
  javaQuiet = false;

  if (strcmp(command, "PING") == 0) {
    Serial.println("PONG");
  } else if (strncmp(command, "SIDE ", 5) == 0) {
    setSideLight(command[5] - '0');
  } else if (strncmp(command, "LCD ", 4) == 0 && strlen(command) >= 6) {
    byte row = command[4] - '0';
    if (row <= 1) {
      showOnLcd(row, command + 6);   // the text starts after "LCD r "
    }
  } else if (strncmp(command, "SOUND ", 6) == 0) {
    playSound(command + 6);
  } else if (strcmp(command, "RFID ON") == 0) {
    setReader(true);
  } else if (strcmp(command, "RFID OFF") == 0) {
    setReader(false);
  }
}
