// Two red lights beside the LCD. They show who has the potato.
// Left = A0 (Player 1), right = D7 (Player 2). LOW turns a light on.

const byte leftLight = A0;
const byte rightLight = 7;

void setupSideLights() {
  pinMode(leftLight, OUTPUT);
  pinMode(rightLight, OUTPUT);
  setSideLight(0);
}

// 0 = both off, 1 = left on, 2 = right on
void setSideLight(byte side) {
  if (side == 1) {
    digitalWrite(leftLight, LOW);
  } else {
    digitalWrite(leftLight, HIGH);
  }
  if (side == 2) {
    digitalWrite(rightLight, LOW);
  } else {
    digitalWrite(rightLight, HIGH);
  }
}
