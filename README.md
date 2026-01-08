# Orion

An Android AI assistant that runs models locally on-device and connects to cloud providers when needed. Built with Kotlin, Jetpack Compose, and llama.cpp.

## Features

### 🤖 Multiple AI Modes
- **Local**: Run GGUF models entirely on-device using llama.cpp
- **Cloud**: Connect to GPT-4, Claude, Gemini, DeepSeek, Qwen, Kimi
- **Live**: Real-time voice conversation via OpenAI Realtime API
- **Emergency**: Mesh networking for offline communication via WiFi Direct

### 🔐 Privacy & Security
- No hardcoded API keys - users provide their own
- Encrypted conversation memory
- Local-first architecture
- No analytics or telemetry
- Open source

### 🆘 Emergency Mode
WiFi Direct mesh network for communication without internet:
- E2E encryption (ECDH + AES-256-GCM)
- Multi-hop message routing
- Store-and-forward protocol
- QR code contact sharing
- Works completely offline

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Local LLM**: llama.cpp (JNI integration)
- **Networking**: OkHttp, WiFi Direct P2P
- **Build**: Gradle with NDK support

## Architecture

```
app/src/main/java/com/orion/proyectoorion/
├── ai/                 # Local LLM engine and llama.cpp bridge
├── data/               # Data persistence and encrypted memory
├── emergency/          # Mesh networking and emergency communications
├── models/             # Cloud provider integrations and model definitions
├── ui/                 # Jetpack Compose screens and components
├── vivo/               # Real-time voice conversation engine
└── viewmodel/          # App logic and state management
```

## Build Instructions

### Prerequisites
- Android Studio Hedgehog or higher
- NDK 27.0.12077973
- CMake 3.22.1
- JDK 11+

### Build Commands
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
./gradlew bundleRelease    # Play Store bundle
```

## Requirements

- **Minimum SDK**: Android 11 (API 30)
- **Architecture**: ARM64
- **Permissions**: Camera, WiFi, Location (for WiFi Direct)

## Development Philosophy

This project uses numbered version snapshots (v1, v2, v3...) with descriptive suffixes:
- `funciona` - in development
- `perfecto` - stable
- `errores` - has bugs (for debugging)
- `estable` - release candidate
- `produccion` - production ready

This approach allows quick rollbacks during rapid C++ integration development.

## Version v99

This version represents a stable foundation with:
- Working llama.cpp integration
- All cloud providers operational
- Functional memory system
- Complete basic UI
- Emergency mesh networking

## Third-Party Libraries

- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Local LLM inference (MIT)
- [ZXing](https://github.com/zxing/zxing) - QR code generation/scanning
- OkHttp - HTTP client
- Jetpack Compose - UI framework

## License

[Add license here]

## Related Projects

- [Genesis Simulation](https://github.com/gamogestionweb/genesis-simulation) - AI personality systems
- [Are You There Reading?](https://github.com/gamogestionweb/Are-you-there-are-reading) - Quantum randomness exploration
- [PCP Universe](https://github.com/gamogestionweb/pcp-universe) - Information persistence theory
- [Physics Discovery AI](https://github.com/gamogestionweb/physics-discovery-ai) - Multi-agent physics discovery 
