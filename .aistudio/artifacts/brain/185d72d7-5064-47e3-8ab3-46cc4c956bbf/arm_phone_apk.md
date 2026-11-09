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

### 🌟 New: Claude Custom Connector (Input Link / MCP Server)

Texidor now includes an **embedded Claude Custom Connector Server** running directly inside the app:

#### 1. ⚡ Input Link for Claude (Model Context Protocol)
- **Embedded MCP Server**: Runs an HTTP & Server-Sent Events (SSE) server on port `8080` (with cleartext traffic allowed and CORS enabled).
- **Copyable Connector Link**:
  - `http://<LAN_IP>:8080/mcp`
  - `http://localhost:8080/mcp` (loopback / ADB reverse)
  - `http://<LAN_IP>:8080/sse` (SSE streaming transport)
- **One-Tap Copy**: Tap **"Claude Link"** in the AI panel to copy the link directly to your clipboard and paste it into Claude's connector prompt.

#### 2. 🔌 Claude Desktop & Claude.ai Integration
- **Claude.ai**: Add a custom integration/connector using the generated input link.
- **Claude Desktop**: Paste this directly into `claude_desktop_config.json`:
  ```json
  {
    "mcpServers": {
      "texidor": {
        "url": "http://<YOUR_DEVICE_IP>:8080/mcp"
      }
    }
  }
  ```

#### 3. 🛠️ Capabilities Claude Obtains via the Connector
- `list_projects`: Inspect all workspaces and projects inside Texidor.
- `get_project_tree`: Traverse folder trees and directories.
- `list_recent_files`: Access all recently edited and opened documents with timestamps.
- `get_active_document`: Read the currently open document in the editor with syntax, line count, and full source code.
- `read_file` & `write_file`: Read any file in workspace or write updates straight into Texidor.
- `execute_terminal_command`: Execute shell commands inside the integrated Ubuntu 24.04 Termux environment (`apt`, `make`, `python3`, `gcc`).
- **Live Activity Log**: Displays all incoming requests from Claude in real-time.

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
