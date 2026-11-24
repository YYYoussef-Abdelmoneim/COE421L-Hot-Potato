// Light sensor on A3. A lower reading means the player's hand is covering it.
// The reading is sent to Java 4 times a second. Java decides if it counts as covered.

unsigned long lastLightTime = 0;

void setupLightSensor() {
  pinMode(A3, INPUT);
}

void checkLightSensor() {
  if (millis() - lastLightTime >= 250) {
    lastLightTime = millis();
    Serial.print("LIGHT ");
    Serial.println(analogRead(A3));
  }
}
