# 📱 Texidor — ARM Release APK for Phones

### 📦 Download & Release Package

This APK is the official, signed **Release** build compiled specifically for **ARM Android phones and handheld devices** (`arm64-v8a` and `armeabi-v7a`):

| Property | Value |
| :--- | :--- |
| **Filename** | **`texidor-release-arm.apk`** |
| **Output Path** | `build-outputs/texidor-release-arm.apk` |
| **Target Architecture** | **ARM 64-bit & 32-bit (`arm64-v8a` / `armeabi-v7a`)** |
| **Target Devices** | Android smartphones (Google Pixel, Samsung Galaxy, OnePlus, Xiaomi, Motorola, Sony Xperia, etc.) |
| **Minimum OS** | Android 7.0+ (API 24) up to Android 15 |
| **Variant** | **Release (Optimized & Signed with release key)** |
| **File Size** | **16 MB** |

---

### 🛡️ Navigation Bar Fix (`III O <` 3-Button Mode Unblocked)

- **Automatic Immersive Hiding**: The traditional 3-button Android navigation bar (`III O <` — Recents, Home, Back) is automatically hidden in sticky immersive mode (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- **Unblocked Bottom Tools**: The bottom coding accessory bar, terminal input prompt, AI assistant prompt, and console sheets now have 100% unobstructed screen space.
- **Swipe-to-Reveal**: Whenever you need the system navigation buttons, swipe up from the bottom edge to transiently reveal them. They automatically slide away after a couple of seconds.
- **Dynamic Inset Safety**: If the buttons are transiently shown, the layout uses dynamic `navigationBarsPadding()` and `imePadding()` so elements never get covered.
- **Toggle Setting**: A toggle labeled **"Hide Navigation Bar (III O <)"** is available in **Themes & Editor Preferences** (palette icon) to customize this behavior anytime.

---

### 🌟 All Core Features Included
- 🤖 **Claude Custom Connector**: Real-time Model Context Protocol (MCP) server running on port 8080.
- 🤖 **GitHub Copilot**: Context-aware coding suggestions and dual assistant panel.
- 🐧 **Ubuntu 24.04 LTS Termux Shell**: Integrated terminal with `apt` package manager and preinstalled coreutils.
- 📜 **Shell, Batch & Make**: Syntax highlighting and script execution.
- 🌐 **Web Preview & TypeScript**: Real-time HTML/CSS/JS preview and on-device TS compilation.
- 🕒 **Recents Tab**: Instant switching between recently edited documents.
- 💾 **Auto-Save**: Periodic background saving to SQLite/Room database every 3 seconds.

---

### 🔨 Host Build Instructions

```bash
# macOS / Linux:
./build.sh --arm --release

# Windows Command Prompt:
build.bat --arm --release

# Make:
make arm
```
Output placed in `./build-outputs/texidor-release-arm.apk`.
