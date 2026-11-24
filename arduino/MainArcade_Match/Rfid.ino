// Parallax RFID reader. It sends each card as: a newline, 10 characters, then a return.
// Its data pin goes to D11. D13 turns it on (LOW = on).
// The reader is only turned on while Java waits for a card.

#include <SoftwareSerial.h>

SoftwareSerial rfid(11, 12);   // receive on D11. D12 is not connected.
const byte rfidEnablePin = 13;
bool readerOn = false;

char cardId[11];               // the card we are reading now (10 characters + end)
byte idLength = 0;
bool readingCard = false;
char lastId[11] = "";          // the card we read last time
unsigned long lastIdTime = 0;
char sentId[11] = "";          // the card we last sent to Java
unsigned long sentTime = 0;

void setupRfid() {
  pinMode(rfidEnablePin, OUTPUT);
  digitalWrite(rfidEnablePin, HIGH);   // reader off
  rfid.begin(2400);
  rfid.stopListening();
}

void setReader(bool on) {
  if (on == readerOn) {
    return;
  }
  readerOn = on;
  readingCard = false;
  if (on) {
    sentId[0] = '\0';                   // allow the same card to be sent again
    rfid.listen();
    digitalWrite(rfidEnablePin, LOW);
  } else {
    digitalWrite(rfidEnablePin, HIGH);
    rfid.stopListening();
  }
}

void checkRfid() {
  if (!readerOn) {
    return;
  }
  while (rfid.available() > 0) {
    char c = rfid.read();
    if (c == '\n') {                     // start of a card
      readingCard = true;
      idLength = 0;
    } else if (c == '\r') {              // end of a card
      if (readingCard && idLength == 10) {
        cardId[10] = '\0';
        cardRead();
      }
      readingCard = false;
    } else if (readingCard && idLength < 10) {
      cardId[idLength] = c;
      idLength++;
    } else {
      readingCard = false;               // too many characters, start again
    }
  }
}

// A card only counts if we read the same ID twice within 1 second.
// While the card stays on the reader, it is not sent again.
void cardRead() {
  unsigned long now = millis();
  bool sameAsLast = (strcmp(cardId, lastId) == 0) && (now - lastIdTime <= 1000);
  if (sameAsLast) {
    bool newCard = (strcmp(cardId, sentId) != 0) || (now - sentTime > 1500);
    if (newCard) {
      Serial.print("CARD ");
      Serial.println(cardId);
      strcpy(sentId, cardId);
    }
    sentTime = now;
  }
  strcpy(lastId, cardId);
  lastIdTime = now;
}
