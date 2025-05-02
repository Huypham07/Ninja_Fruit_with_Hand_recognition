# Ninja Fruit Game

**Ninja Fruit** is an interactive fruit-slicing game that uses hand recognition powered by MediaPipe/ML Kit Pose Estimation. The game detects hand landmarks to enable a unique, touch-free gaming experience.

## Features

- **Two Game Modes:**
  - **Touch Mode:** Traditional gameplay using touch controls.
  - **Tracking Mode:** Play using your hand tracked via the camera.

- **Game Interactions:**
  - Special effects like:
    - **Freeze:** Slow down the game.
    - **Bomb All:** Slice all fruits at once.
    - **Shield:** Protect yourself from bombs.

- **Customization:**
  - Changeable backgrounds.
  - **Versus mode:** Available only in the Android version.
    - Available in Touch Mode on all platforms.
    - Tracking-based Versus Mode is only available on the Android version and requires a relatively powerful device (e.g., Snapdragon 845 or better).

## Versions

### 1️⃣ Web Version (JavaScript + MediaPipe Hand)

- All core features implemented at a basic level.
- Requires a good-quality device for smooth gameplay.
- Not highly optimized; performance may vary.

### 2️⃣ Android Version (Java/Kotlin + ML Kit Pose Estimation)

- Fully featured and optimized for smooth performance across most Android devices.
- Better overall experience compared to the web version.
- Includes full support for game interactions and versus mode:
  - Touch-based versus mode.
  - Tracking-based versus mode is supported but requires a device with decent hardware performance to ensure smooth gameplay.

> **Note:**  
> For future development of tracking-based versus mode, devices need good hardware support, and improvements in detection models are required, as ML Kit and MediaPipe currently have limitations with multi-object detection.

## Installation

1. **Clone the repository:**

2. **Structure:**
- `Ninja_Fruit_Game/` → Android version.
- `Ninja_Fruit_Web/` → Web version.

3. **Run:**
- **Web Version:**
  - Open `index.html` in a browser.
- **Android Version:**
  - Open the project in Android Studio and run it on a device or emulator.
  - Alternatively, download the APK from the Releases section and install it directly on your Android device.
