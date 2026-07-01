<div align = "center">
<h1>Moodlit</h1>
<p>A smart ambient lamp that signals who is calling — without needing to check your phone.</p>

![Android](https://img.shields.io/badge/Android-Java-3DDC84?logo=android&logoColor=white)
![ESP8266](https://img.shields.io/badge/ESP8266-NodeMCU-blue?logo=espressif)
![Local Wi-Fi](https://img.shields.io/badge/Network-Local%20Wi--Fi-orange)

</div>

## What Is Moodlit?

Moodlit is an IoT system for **emotional boundary awareness**. When your phone rings, an Android app identifies the caller's category and instantly lights up a corresponding colored LED on a NodeMCU ESP8266 over your local Wi-Fi.

## How It Works

```
Incoming Call
      ↓
Android App
  → resolves caller category from saved mappings
  → checks DND / anxiety-free mode
      ↓
GET http://{esp_ip}/trigger?cat=toxic
      ↓
NodeMCU ESP8266
      ↓
LED lights up + pulses
```

When the call ends → `GET /idle` → all LEDs off.

---

## Features

- **Direct Android → ESP communication** — no Flask server, no cloud
- **DND per category** — block any individual category from lighting the lamp
- **Anxiety-free mode** — auto-blocks toxic + work categories globally
- **Multi-contact picker** — bulk assign contacts to a category at once
- **Boot persistence** — `BootReceiver` restarts call monitoring after device reboot
- **System notification** — shows category label on incoming call
- **Local call log** — last 50 entries stored on-device via `SharedPreferences`
- **Pulse urgency** — repeat calls from the same number blink the LED faster
- **Auto-idle watchdog** — ESP turns all LEDs off after 60s if no command received

---

## LED Color Map

| Category | Color  | LED Pin     |
|----------|--------|-------------|
| toxic    | 🔴 Red    |  D1  |
| work     | 🔵 Blue   |  D2  |
| friends  | 🟡 Yellow |  D5 |
| family   | 🟢 Green  |  D6 |

---


## Hardware Required

| Component | Details |
|-----------|---------|
| NodeMCU ESP8266 | Any standard board |
| LEDs × 4 | Red, Blue, Yellow, Green |
| Resistors × 4 | 220Ω (one per LED) |
| Jumper wires | — |
| Android phone | API 26+ (Android 8.0+) |

### Wiring

```
ESP8266 Pin → Resistor (220Ω) → LED (+) → LED (-) → GND
D1 → Red LED
D2 → Blue LED
D5 → Yellow LED
D6 → Green LED
```

---

## Project Structure

```
Moodlit/
├── app/                                    # Android app
│   └── src/main/java/com/lamp/moodlit/
│       ├── MainActivity.java               # UI: map numbers → categories, save ESP IP
│       ├── CallMonitorService.java         # Foreground service: detects calls, fires HTTP to ESP
│       ├── UserDashboardActivity.java      # DND toggles, anxiety-free mode, local call log
│       ├── ContactPickerActivity.java      # Bulk contact picker for category assignment
│       ├── NotifHelper.java                # System notification on incoming call
│       └── BootReceiver.java               # Restarts call monitor after device reboot
├── esp/
│   └── moodlit_esp/                        # Arduino sketch for NodeMCU ESP8266
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Setup

### 1 — Flash the ESP

1. Open `esp/moodlit_esp/moodlit_esp.ino` in Arduino IDE
2. Install board: **ESP8266 by ESP8266 Community** via Board Manager
3. Set your Wi-Fi credentials in the sketch (`ssid` / `password`)
4. Flash to NodeMCU
5. Open Serial Monitor (115200 baud) — note the **IP address** printed on boot

### 2 — Install the Android App

1. Open the project in Android Studio
2. Build and install on your Android device (`Run → Run 'app'`)

### 3 — Configure ESP IP

1. Open the Moodlit app
2. Enter the ESP's IP address (from Serial Monitor) in the **Server IP** field
3. Tap **Test Connection** — you should see `pong` response

### 4 — Map Your Contacts

1. Tap **Add Contact** or use the **multi-picker** for bulk assign
2. Assign each contact to a category: `toxic` / `work` / `friends` / `family`

### 5 — Test

Call your phone from a mapped number. The corresponding LED should light up immediately.

---

## ESP Endpoints

| Endpoint | Action |
|----------|--------|
| `GET /trigger?cat={category}` | Light corresponding LED + start pulse |
| `GET /idle` | Turn all LEDs off |
| `GET /ping` | Returns `pong` — used for connection check |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android App | Java, Android SDK (API 26+) |
| HTTP Client | `HttpURLConnection` (no external library) |
| Local Storage | `SharedPreferences` |
| ESP Firmware | Arduino C++ (ESP8266 SDK) |
| Communication | HTTP over local Wi-Fi |