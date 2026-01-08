# 🌟 ORION - AI Assistant

Advanced AI assistant with multiple engines (Cloud, Local, Live) and **Emergency Mode** with mesh communication.

## 🆕 EMERGENCY MODE (New)

Internet-free communication via WiFi Direct mesh network. Perfect for:
- 🆘 Emergencies and natural disasters
- 📵 Areas without mobile coverage
- 🔒 Private encrypted communication

### Features

- **WiFi Direct P2P**: Direct device-to-device connection without router
- **E2E Encryption**: ECDH + AES-256-GCM for maximum security
- **Mesh Networking**: Messages hop between devices until reaching destination
- **Store-and-Forward**: Messages are automatically stored and forwarded
- **High TTL (25 hops)**: Coverage for metropolitan areas like Madrid
- **Optimized Messages**: Maximum 140 characters for efficiency

### How It Works

1. **Initial Setup (one-time)**:
   - Open Orion → Emergency Mode
   - Tap "Share my code"
   - Send code to family via WhatsApp
   - They add you as emergency contact

2. **In case of emergency**:
   - Open Orion → Emergency Mode
   - Tap "ACTIVATE NETWORK"
   - Use quick buttons:
     - 🆘 **SOS**: Emergency alert (broadcast to all)
     - ✅ **I'm safe**: Notify you're safe
     - 💬 **Message**: Send custom text

3. **Mesh Propagation**:
   - Your message hops between nearby devices
   - Each device acts as repeater
   - Message reaches destination even if recipient isn't nearby

### Technical Architecture

```
📱 Your device
    ↓ WiFi Direct (E2E encrypted)
📱 Nearby device A
    ↓ Store & Forward
📱 Nearby device B
    ↓ Multi-hop routing
📱 Family member's device
```

### Module Files

```
emergency/
├── EmergencyCrypto.kt     # ECDH + AES-GCM encryption
├── MeshEmergency.kt       # Mesh network engine
├── EmergencyContacts.kt   # Contact management
├── EmergencyStrings.kt    # Multi-language strings
└── EmergencyUI.kt         # Jetpack Compose interface
```

---

## 🚀 Orion Modes

### ☁️ CLOUD
- GPT-4, Claude, Gemini, DeepSeek, Qwen, Kimi
- Automatic persistent memory
- Maximum power

### 📱 LOCAL (Offline)
- GGUF models optimized for mobile
- Total privacy
- No internet required

### 👁️ LIVE (Realtime)
- OpenAI Realtime API
- Duplex voice conversation
- Instant response

### 🆘 EMERGENCY (Mesh)
- WiFi Direct P2P
- End-to-end encryption
- Works without internet or data

---

## 📋 Requirements

- Android 8.0+ (API 26)
- WiFi Direct compatible
- Location permissions (for WiFi Direct)

## 🔧 Required Permissions (AndroidManifest.xml)

```xml
<!-- WiFi Direct -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

<uses-feature android:name="android.hardware.wifi.direct" android:required="false" />
```

## 🔧 Build

```bash
./gradlew assembleDebug
```

---

**Keep your family connected even without internet!** 🆘📡
