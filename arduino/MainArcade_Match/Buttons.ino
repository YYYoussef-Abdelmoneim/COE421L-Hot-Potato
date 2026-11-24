// Four arcade buttons. Each one connects its pin to ground when pressed.
// Button 1 = blue (D2), 2 = green (D3), 3 = red (D4), 4 = yellow (D5).

const byte buttonPins[4] = {2, 3, 4, 5};
bool buttonDown[4];              // the state we last sent to Java
bool lastReading[4];             // the last raw reading
unsigned long changeTime[4];     // when the raw reading last changed

void setupButtons() {
  for (byte i = 0; i < 4; i++) {
    pinMode(buttonPins[i], INPUT_PULLUP);   // the pin reads LOW when the button is pressed
    buttonDown[i] = (digitalRead(buttonPins[i]) == LOW);
    lastReading[i] = buttonDown[i];
    changeTime[i] = 0;
  }
}

// A new state must stay the same for 25 ms before we send it.
// This stops one press from looking like many (switch bounce).
void checkButtons() {
  for (byte i = 0; i < 4; i++) {
    bool pressed = (digitalRead(buttonPins[i]) == LOW);
    if (pressed != lastReading[i]) {
      lastReading[i] = pressed;
      changeTime[i] = millis();
    }
    if (pressed != buttonDown[i] && millis() - changeTime[i] >= 25) {
      buttonDown[i] = pressed;
      Serial.print("BTN ");
      Serial.print(i + 1);
      if (pressed) {
        Serial.println(" DOWN");
      } else {
        Serial.println(" UP");
      }
    }
  }
}
