# Orion

**A Personal AI Assistant That Truly Belongs to You**

Orion is an attempt to build a personal AI that operates independently of cloud services and subscriptions. It's software that runs on mobile devices, keeps data private, and continues operating even when network connectivity fails.

This repository contains v99, an early version representing the foundation for subsequent development.

---

## Core Features

- Runs AI models locally on device (llama.cpp integration)
- Connects to cloud providers when additional compute is needed
- Maintains encrypted conversation memory
- Operates offline in emergencies using mesh networks
- Supports multiple languages

---

## Development Philosophy

### Iterative Versioning with ZIP Snapshots

The project uses numbered snapshots (v1, v2, v3...) instead of traditional git branching.

**Descriptive suffixes:**
- `funciona` = in development
- `perfecto` = stable/working
- `errores` = contains bugs (for debugging)
- `estable` = release candidate
- `produccion` = production-ready

**Advantages:** Quick rollbacks, clear documentation, easy comparison, creative process preservation.

### Rationale

During rapid iteration with native C++ integration, breaking changes occur frequently. Numbered snapshots guarantee a known working state, enabling free experimentation.

### Version v99

Represents a solid foundation with:
- Core llama.cpp integration working
- All cloud providers operational
- Memory system operational
- Complete basic UI

---

## Technical Architecture

```
com.orion.proyecto/
├── ai/
│   ├── LlamaAndroid.kt (JNI bridge to llama.cpp)
│   └── LocalLLMEngine.kt (local inference management)
├── data/
│   ├── DataManager.kt (SharedPreferences & files)
│   └── AIPrivateMemory.kt (encrypted AI memory)
├── emergency/
│   ├── MeshEmergency.kt (Bluetooth/WiFi mesh network)
│   └── EmergencyCrypto.kt (encryption utilities)
├── models/
│   ├── CloudProviders.kt (cloud API integrations)
│   ├── CloudModels.kt (available cloud models)
│   └── LocalModels.kt (downloadable local models)
├── ui/
│   └── screens/ (all app screens)
│       └── viewmodel/
│           └── BrainViewModel.kt (main app logic)
└── vivo/ (Voice/Live Mode)
    └── OrionVivoEngine.kt (continuous conversation)
```

---

## Build Instructions

```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
./gradlew bundleRelease    # Play Store bundle
```

---

## Requirements

- Android Studio Hedgehog or higher
- NDK 27.0.12077973
- CMake 3.22.1
- JDK 11+
- Target device: Android 11+ (API 30+), ARM64

---

## Privacy & Security

- **No hardcoded API keys:** Users provide their own keys, stored locally
- **Local-first approach:** AI can run entirely on-device
- **Encrypted memory:** Conversation history uses device encryption
- **No tracking:** No analytics or telemetry

---

## Third-Party Libraries

- [llama.cpp](https://github.com/ggerganov/llama.cpp) — Local LLM inference (MIT License)
- OkHttp, Jetpack Compose, and other Android libraries

---

## Voice Preservation Technology

The project explores voice cloning technology for preserving the voices of loved ones who are no longer present. **Legacy Mode** enables this through AI-based voice synthesis, representing a technical approach to maintaining human connections across time.

This feature addresses the fundamental human need to preserve memories and maintain emotional connections with those we've lost.

---

## Related Projects

This project is part of a broader exploration of AI, consciousness, and human connection:

### [Genesis Simulation](https://github.com/gamogestionweb/genesis-simulation)
Creates AIs with unique personalities (Adam, Eve). Orion applies the same principle: an AI that belongs to the user, understands context, and evolves through interaction.

### [Are You There Reading?](https://github.com/gamogestionweb/Are-you-there-are-reading)
Explores whether chance truly exists. Orion demonstrates this practically: every conversation with a personal AI is unique and unrepeatable.

### [PCP Universe](https://github.com/gamogestionweb/pcp-universe)
Based on the principle that information is never destroyed, explores how data persists in the universe. Orion attempts to preserve conversational data through encrypted memory systems.

### [Physics Discovery AI](https://github.com/gamogestionweb/physics-discovery-ai)
Multi-agent systems deriving physical laws from first principles—exploring how intelligence discovers truth.

---

*This repository represents the foundation of ongoing development.*
