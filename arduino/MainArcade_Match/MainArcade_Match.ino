// Hot Potato main board (LilyPad Arduino).
// It reads the buttons, the RFID reader and the light sensor, and sends what happens to Java.
// It also runs the LCD, the two red side lights and the two buzzers when Java asks.
// Java makes all the game decisions.
// The other tabs hold the code for each part.

unsigned long lastCommandTime = 0;   // when Java last sent a command
bool javaQuiet = false;              // true when Java has not sent anything for a while

void setup() {
  Serial.begin(9600);   // the XBee radio is on the hardware serial pins (D0 and D1)
  setupButtons();
  setupSideLights();
  setupSounds();
  setupRfid();
  setupLightSensor();
  setupLcd();
  Serial.println("READY");
}

void loop() {
  checkButtons();
  checkLightSensor();
  checkRfid();
  updateSounds();
  readCommands();

  // Java sends something every second. If it stops for 4 seconds, turn everything off.
  if (!javaQuiet && millis() - lastCommandTime > 4000) {
    javaQuiet = true;
    setSideLight(0);
    setReader(false);
    stopAllSounds();
  }
}
