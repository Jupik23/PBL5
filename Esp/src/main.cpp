#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run make menuconfig to enable it
#endif

BluetoothSerial SerialBT;

unsigned long previousMillis = 0; // Do śledzenia czasu
const unsigned long interval = 5000; // Czas w milisekundach (5 sekund)

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32test");  
  Serial.println("The device started, now you can pair it with Bluetooth!");
}

void loop() {
  // Sprawdzanie danych z Serial
  if (Serial.available()) {
    char received = Serial.read();
    SerialBT.write(received);
    Serial.print("Wysłano przez Bluetooth: ");
    Serial.println(received);
  }

  // Sprawdzanie danych z Bluetooth
  if (SerialBT.available()) {
    char receivedBT = SerialBT.read();
    Serial.write(receivedBT);
    Serial.print("Odebrano z Bluetooth: ");
    Serial.println(receivedBT);
  }

  // Wysyłanie danych co 5 sekund
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;

    float T1 = random(200, 400) / 10.0; // Przykładowa temperatura T1 (20.0 - 40.0 °C)
    float T2 = random(200, 400) / 10.0;
    float T3 = random(200, 400) / 10.0; 
    float Voltage = random(300, 500) / 100.0; 
    float Current = random(100, 200) / 100.0;

    String data = "(" + String(T1, 1) + "," + String(T2, 1) + "," + String(T3, 1) + 
                  "," + String(Voltage, 2) + "," + String(Current, 2) + ")";
    
    // Wysyłanie przez Bluetooth
    SerialBT.print(data);
    Serial.print("Wysłano dane przez Bluetooth: ");
    Serial.println(data);
  }
  delay(20); 
}
