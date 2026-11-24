// 16x2 LCD with an I2C backpack at address 0x27 (SDA = A4, SCL = A5).

#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>

hd44780_I2Cexp lcd(0x27);
bool lcdWorks = false;
unsigned long lastLcdTry = 0;

void setupLcd() {
  Wire.begin();
  Wire.setWireTimeout(25000, true);   // do not freeze if an LCD wire is loose
  startLcd();
  if (lcdWorks) {
    lcd.print("HOT POTATO");
  }
}

void startLcd() {
  lastLcdTry = millis();
  lcdWorks = (lcd.begin(16, 2) == 0);
  if (lcdWorks) {
    lcd.backlight();
  }
}

// Writes text on one row and fills the rest of the row with spaces.
void showOnLcd(byte row, const char *text) {
  if (!lcdWorks) {
    if (millis() - lastLcdTry < 5000) {
      return;     // the LCD did not start; try again every 5 seconds
    }
    startLcd();
    if (!lcdWorks) {
      return;
    }
  }
  lcd.setCursor(0, row);
  byte count = 0;
  while (text[count] != '\0' && count < 16) {
    lcd.write(text[count]);
    count++;
  }
  while (count < 16) {
    lcd.write(' ');
    count++;
  }
}
