# 🚀 Texidor — Release APKs & Build System

All 3 official **Release APKs** have been compiled and signed with the release key. They are ready for deployment in `./build-outputs/`:

### 📦 Release APK Artifacts

| Artifact File | Target Architecture | Recommended Devices | Size | Build Variant |
| :--- | :--- | :--- | :--- | :--- |
| **`texidor-release-arm.apk`** | **ARM (`arm64-v8a` / `armeabi-v7a`)** | **Android Smartphones & Handhelds** | **16 MB** | **Release (Signed)** |
| **`texidor-release-uni.apk`** | **Universal** | All Android devices & platforms (API 24+) | **16 MB** | **Release (Signed)** |
| **`texidor-release-x86.apk`** | **x86 / x86-64** | ChromeOS, Intel/AMD Android tablets, Emulators | **16 MB** | **Release (Signed)** |

---

### ⚡ Claude Custom Connector (Input Link & MCP)

Texidor includes a built-in **Claude Custom Connector Server** implementing Anthropic's **Model Context Protocol (MCP)**:

1. **Connector Input Link**:
   - `http://<LAN_IP>:8080/mcp`
   - `http://localhost:8080/mcp`
2. **Features**:
   - Copyable connector link directly inside the app under the **Claude Link** tab.
   - Ready-to-copy Claude Desktop JSON configuration.
   - Claude can list projects, inspect directory trees, view recently edited files, read/write active editor buffers, and run commands in the integrated Ubuntu/Termux shell.
   - Real-time activity log showing every tool called by Claude.

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
