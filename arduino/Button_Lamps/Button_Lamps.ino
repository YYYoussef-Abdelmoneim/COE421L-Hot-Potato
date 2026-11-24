// Second LilyPad: turns the four button lamps on and off.
// Java sends one hex digit (0 to F) over USB. Each bit is one lamp:
// 1 = blue (D2), 2 = green (D3), 4 = red (D4), 8 = yellow (D5).
// For example, 5 = blue + red. A = green + yellow.
// Java also sends '?' every second. If it stops for 3 seconds, all lamps turn off.
// Each lamp has its own resistor to +5 V, so LOW turns a lamp on.

const byte lampPins[4] = {2, 3, 4, 5};
unsigned long lastHeartbeat = 0;

void setup() {
  for (byte i = 0; i < 4; i++) {
    pinMode(lampPins[i], OUTPUT);
    digitalWrite(lampPins[i], HIGH);   // lamp off
  }
  Serial.begin(9600);
}

void showLamps(byte mask) {
  for (byte i = 0; i < 4; i++) {
    if (mask & (1 << i)) {
      digitalWrite(lampPins[i], LOW);
    } else {
      digitalWrite(lampPins[i], HIGH);
    }
  }
}

void loop() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    if (c == '?') {
      lastHeartbeat = millis();
      Serial.println("LAMPS OK");
    } else if (c >= '0' && c <= '9') {
      showLamps(c - '0');
    } else if (c >= 'A' && c <= 'F') {
      showLamps(c - 'A' + 10);
    }
  }
  if (millis() - lastHeartbeat > 3000) {
    showLamps(0);
  }
}
