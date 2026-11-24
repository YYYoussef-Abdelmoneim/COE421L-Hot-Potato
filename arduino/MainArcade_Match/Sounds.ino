// Two buzzers. D6 plays the game sounds. D10 makes the potato tick.
// They use different timers inside the chip, so both can sound at the same time.
// HIGH keeps a buzzer quiet.

const byte gameBuzzer = 6;
const byte tickBuzzer = 10;

// A sound is a list of notes. Each note is {pitch in Hz, length in ms}. Pitch 0 = silence.
int startNotes[][2] = {{2093, 90}, {0, 35}, {3136, 130}};
int hitNotes[][2] = {{3520, 80}};
int missNotes[][2] = {{2093, 130}, {0, 45}, {2093, 130}};
int warnNotes[][2] = {{4186, 120}, {0, 65}, {4186, 120}, {0, 65}, {4186, 120}};
int passNotes[][2] = {{2093, 100}, {2637, 100}, {3136, 100}, {4186, 200}};
int dropNotes[][2] = {{4186, 130}, {3136, 130}, {2637, 130}, {2093, 260}};

int (*melody)[2] = NULL;      // the sound playing now (NULL = none)
byte melodyLength = 0;
byte noteIndex = 0;
unsigned long noteEndTime = 0;
bool melodyImportant = false;  // a small sound (HIT, MISS) cannot cut an important one
bool toneOn = false;
bool tickOn = false;
unsigned long tickEndTime = 0;

void setupSounds() {
  pinMode(gameBuzzer, OUTPUT);
  pinMode(tickBuzzer, OUTPUT);
  digitalWrite(gameBuzzer, HIGH);
  digitalWrite(tickBuzzer, HIGH);
}

void playSound(const char *name) {
  if (strcmp(name, "TICK") == 0) startTick();
  else if (strcmp(name, "OFF") == 0) stopAllSounds();
  else if (strcmp(name, "START") == 0) startMelody(startNotes, 3, true);
  else if (strcmp(name, "HIT") == 0) startMelody(hitNotes, 1, false);
  else if (strcmp(name, "MISS") == 0) startMelody(missNotes, 3, false);
  else if (strcmp(name, "WARN") == 0) startMelody(warnNotes, 5, true);
  else if (strcmp(name, "PASS") == 0) startMelody(passNotes, 4, true);
  else if (strcmp(name, "DROP") == 0) startMelody(dropNotes, 4, true);
}

void startMelody(int notes[][2], byte length, bool important) {
  if (melody != NULL && melodyImportant && !important) {
    return;
  }
  melody = notes;
  melodyLength = length;
  melodyImportant = important;
  noteIndex = 0;
  playNote();
}

void playNote() {
  int hz = melody[noteIndex][0];
  int ms = melody[noteIndex][1];
  if (hz > 0) {
    tone(gameBuzzer, hz);
    toneOn = true;
  } else {
    quietGameBuzzer();
  }
  noteEndTime = millis() + ms;
}

void quietGameBuzzer() {
  if (toneOn) {
    noTone(gameBuzzer);   // only call noTone when a tone is playing
    toneOn = false;
  }
  digitalWrite(gameBuzzer, HIGH);
}

// Called every loop: moves to the next note and ends the tick.
void updateSounds() {
  if (tickOn && millis() >= tickEndTime) {
    stopTick();
  }
  if (melody != NULL && millis() >= noteEndTime) {
    noteIndex++;
    if (noteIndex >= melodyLength) {
      melody = NULL;
      quietGameBuzzer();
    } else {
      playNote();
    }
  }
}

// The tick is a 30 ms beep at 2300 Hz on D10.
// These lines set up Timer1 to flip pin D10 on and off at that speed.
void startTick() {
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;
  OCR1A = F_CPU / (2UL * 8 * 2300) - 1;
  OCR1B = OCR1A;
  TCCR1A = _BV(COM1B0);
  TCCR1B = _BV(WGM12) | _BV(CS11);
  tickOn = true;
  tickEndTime = millis() + 30;
}

void stopTick() {
  TCCR1A = 0;
  TCCR1B = 0;
  digitalWrite(tickBuzzer, HIGH);
  tickOn = false;
}

void stopAllSounds() {
  melody = NULL;
  quietGameBuzzer();
  stopTick();
}
