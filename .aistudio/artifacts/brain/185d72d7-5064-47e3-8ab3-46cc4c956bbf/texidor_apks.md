# 🚀 Texidor — Release APKs & Build System

All 3 official **Release APKs** have been compiled, signed, and updated in `./build-outputs/`:

### 📦 Release APK Artifacts

| Artifact File | Target Architecture | Recommended Devices | Size | Build Variant |
| :--- | :--- | :--- | :--- | :--- |
| **`texidor-release-arm.apk`** | **ARM (`arm64-v8a` / `armeabi-v7a`)** | **Android Smartphones & Handhelds** | **16 MB** | **Release (Signed)** |
| **`texidor-release-uni.apk`** | **Universal** | All Android devices & platforms (API 24+) | **16 MB** | **Release (Signed)** |
| **`texidor-release-x86.apk`** | **x86 / x86-64** | ChromeOS, Intel/AMD Android tablets, Emulators | **16 MB** | **Release (Signed)** |

---

### 🛡️ 3-Button Navigation Bar Auto-Hiding (`III O <`)

- **Eliminates Bottom Screen Overlap**: The traditional Android navigation bar (`III O <`) is hidden in sticky immersive mode so it no longer obstructs the coding accessory bar, terminal command line, AI chat input, or bottom sheet controls.
- **Swipe-to-Reveal**: Swipe up from the bottom edge to temporarily show the system buttons.
- **Dynamic Inset Padding**: When the buttons or keyboard appear, layout contents dynamically shift up using `navigationBarsPadding()` and `imePadding()`.
- **Preference Toggle**: Can be toggled on/off under **Themes & Editor Preferences** → **"Hide Navigation Bar (III O <)"**.

---

### 💻 Cross-Platform Build Commands

```bash
# macOS & Linux (Bash)
./build.sh --arm --release    # ARM for phones
./build.sh --x86 --release    # x86-64
./build.sh --uni --release    # Universal

# Windows Command Prompt (Batch)
build.bat --arm --release

# Make
make arm
```
