#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include "secrets.h"

ESP8266WebServer server(80);

#define LED_RED    D1
#define LED_BLUE   D2
#define LED_YELLOW D5
#define LED_GREEN  D6

int currentLED = -1;
int pulseCount = 0;
bool pulsing = false;
unsigned long lastPulse = 0;
unsigned long lastCommandTime = 0;
bool ledState = false;

const unsigned long IDLE_TIMEOUT = 60000;

void allOff() {
  digitalWrite(LED_RED, LOW);
  digitalWrite(LED_BLUE, LOW);
  digitalWrite(LED_YELLOW, LOW);
  digitalWrite(LED_GREEN, LOW);
  pulsing = false;
  pulseCount = 0;
  currentLED = -1;
  ledState = false;
}

void handleTrigger() {
  String cat = server.arg("cat");
  int pin = -1;

  if (cat == "toxic") pin = LED_RED;
  else if (cat == "work") pin = LED_BLUE;
  else if (cat == "friends") pin = LED_YELLOW;
  else if (cat == "family") pin = LED_GREEN;

  if (pin == -1) {
    server.send(400, "text/plain", "Unknown category");
    return;
  }

  if (currentLED == pin) {
    pulseCount++;
  } else {
    allOff();
    currentLED = pin;
    pulseCount = 1;
    lastPulse = 0;
  }

  pulsing = true;
  lastCommandTime = millis();
  server.send(200, "text/plain", "OK");
}

void handleIdle() {
  allOff();
  server.send(200, "text/plain", "Idle");
}

void handlePing() {
  server.send(200, "text/plain", "pong");
}

void setup() {
  Serial.begin(115200);

  pinMode(LED_RED, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);
  pinMode(LED_YELLOW, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  allOff();

  WiFi.begin(ssid, password);
  Serial.print("Connecting");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nConnected! IP: " + WiFi.localIP().toString());

  server.on("/trigger", handleTrigger);
  server.on("/idle", handleIdle);
  server.on("/ping", handlePing);
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  server.handleClient();

  if (pulsing && currentLED != -1) {
    int speed = max(80, 500 - (pulseCount * 80));

    if (millis() - lastPulse > (unsigned long)speed) {
      ledState = !ledState;
      digitalWrite(currentLED, ledState ? HIGH : LOW);
      lastPulse = millis();
    }

    if (millis() - lastCommandTime > IDLE_TIMEOUT) {
      allOff();
    }
  }
}