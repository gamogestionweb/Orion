# Orion

This is where it all started.

Orion is my attempt to build a personal AI that truly belongs to you. Not a service, not a subscription — software that runs on your phone, keeps your data private, and works even when everything else fails.

This repository contains v99, an early version. It's messy, it's real, and it's the foundation for everything that came after.

## What Orion does

- Runs AI models locally on your device (llama.cpp)
- Connects to cloud providers when you want more power
- Remembers conversations with encrypted memory
- Works offline in emergencies via mesh networking
- Speaks your language

## Development Philosophy

### Iterative Versioning with ZIP Snapshots

This project follows a **snapshot-based development approach** rather than traditional git branching:

1. Each working state is saved as a numbered ZIP file (v1, v2, v3...)
2. Version names include descriptive suffixes:
   - `funciona` = working
   - `perfecto` = stable/perfect
   - `errores` = has errors (kept for debugging reference)
   - `estable` = stable release candidate
   - `produccion` = ready for production

This approach allows for:
- **Quick rollbacks** without git complexity
- **Clear documentation** of what each version achieved
- **Easy comparison** between versions
- **Preservation** of the complete creative process

### Why This Works

When you're iterating rapidly on a mobile app with native C++ integration, things break. A lot. Having numbered snapshots means:

- You always have a known-good state to return to
- You can experiment freely without fear
- The version name itself tells you what was working

### Version v99

This version (v99) represents a stable foundation with:
- Core llama.cpp integration working
- All cloud providers functional
- Memory system operational
- Basic UI complete

## Technical Architecture

```
com.orion.proyectoorion/
├── ai/                    # Local LLM integration
│   ├── LlamaAndroid.kt    # JNI bridge to llama.cpp
│   └── LocalLLMEngine.kt  # Local inference management
├── data/                  # Persistence layer
│   ├── DataManager.kt     # SharedPreferences & file management
│   ├── AIPrivateMemory.kt # Encrypted AI memory
│   └── Languages.kt       # i18n support
├── emergency/             # Emergency communication
│   ├── MeshEmergency.kt   # Bluetooth/WiFi mesh network
│   └── EmergencyCrypto.kt # Encryption utilities
├── models/                # AI model configurations
│   ├── CloudProviders.kt  # Cloud API integrations
│   ├── CloudModels.kt     # Available cloud models
│   └── LocalModels.kt     # Downloadable local models
├── ui/                    # Jetpack Compose UI
│   └── screens/           # All app screens
├── viewmodel/
│   └── BrainViewModel.kt  # Main app logic
└── vivo/                  # Voice/Live mode
    └── OrionVivoEngine.kt # Continuous conversation
```

## Build Instructions

```bash
# Open in Android Studio
# OR build from command line:

./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
./gradlew bundleRelease    # Play Store bundle
```

### Requirements

- Android Studio Hedgehog or later
- NDK 27.0.12077973
- CMake 3.22.1
- JDK 11+
- Target device: Android 11+ (API 30+), ARM64

## Privacy & Security

- **No hardcoded API keys**: Users provide their own API keys stored locally
- **Local-first**: AI can run entirely on-device
- **Encrypted memory**: Conversation history uses device encryption
- **No tracking**: No analytics or telemetry

## License

This project showcases the development process. The code is provided for educational purposes.

## Third-Party Libraries

- [llama.cpp](https://github.com/ggerganov/llama.cpp) - Local LLM inference (MIT License)
- OkHttp, Jetpack Compose, and other Android libraries

---

## Connections

<table>
<tr>
<td width="50%">

### Talking to those who are gone

> *"Where are you, mom?"*

In **[100 Días de una Vida](https://github.com/gamogestionweb/100-dias-de-una-vida)**, Abel asks where his dead mother is. Orion was born from that pain: creating a way to hear the voices of those we've lost.

Legacy Mode is not magic. It's technology born from loss.

</td>
<td width="50%">

### Personal AI vs massive AI

**[Genesis Simulation](https://github.com/gamogestionweb/genesis-simulation)** creates AIs with unique personalities (Adam, Eve). Orion does the same: an AI that belongs to you, knows you, evolves with you.

Not one massive intelligence. Millions of personal intelligences.

</td>
</tr>
<tr>
<td>

### The chance of meeting

> *"How many things had to happen for you to read this?"*

**[Are You There Reading?](https://github.com/gamogestionweb/Are-you-there-are-reading)** asks if chance exists. Orion is the practical answer: each conversation with your personal AI is a unique, unrepeatable event that only exists because you existed.

</td>
<td>

### The physics of memory

If **[PCP Universe](https://github.com/gamogestionweb/pcp-universe)** is right and information is never destroyed, then the voices of those we've lost still exist, encoded somewhere in the universe.

Orion tries to recover a fragment of that.

</td>
</tr>
</table>

---

*This is just the beginning.*
