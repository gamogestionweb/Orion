# 🌟 ORION - AI Assistant

Asistente de IA avanzado con múltiples motores (Cloud, Local, Vivo) y **Modo Emergencia** con comunicación mesh.

## 🆕 MODO EMERGENCIA (Nuevo)

Comunicación sin Internet mediante red mesh WiFi Direct. Perfecto para:
- 🆘 Emergencias y desastres naturales
- 📵 Zonas sin cobertura móvil
- 🔒 Comunicación privada cifrada

### Características

- **WiFi Direct P2P**: Conexión directa entre dispositivos sin router
- **Cifrado E2E**: ECDH + AES-256-GCM para máxima seguridad
- **Mesh Networking**: Los mensajes saltan entre dispositivos hasta llegar al destino
- **Store-and-Forward**: Los mensajes se almacenan y reenvían automáticamente
- **TTL Alto (25 saltos)**: Cobertura para áreas metropolitanas como Madrid
- **Mensajes Optimizados**: Máximo 140 caracteres para eficiencia

### Cómo Funciona

1. **Configuración Inicial (una vez)**:
   - Abre Orion → Modo Emergencia
   - Pulsa "Compartir mi código"
   - Envía el código a tu familia por WhatsApp
   - Ellos te añaden como contacto de emergencia

2. **En caso de emergencia**:
   - Abre Orion → Modo Emergencia
   - Pulsa "ACTIVAR RED"
   - Usa los botones rápidos:
     - 🆘 **SOS**: Alerta de emergencia (broadcast a todos)
     - ✅ **Estoy bien**: Notifica que estás a salvo
     - 💬 **Mensaje**: Envía texto personalizado

3. **Propagación Mesh**:
   - Tu mensaje salta entre dispositivos cercanos
   - Cada dispositivo actúa como repetidor
   - El mensaje llega incluso si el destinatario no está cerca

### Arquitectura Técnica

```
📱 Tu dispositivo
    ↓ WiFi Direct (cifrado E2E)
📱 Dispositivo cercano A
    ↓ Store & Forward
📱 Dispositivo cercano B
    ↓ Multi-hop routing
📱 Dispositivo de tu familia
```

### Archivos del Módulo

```
emergency/
├── EmergencyCrypto.kt     # Cifrado ECDH + AES-GCM
├── MeshEmergency.kt       # Motor de red mesh
├── EmergencyContacts.kt   # Gestión de contactos
├── EmergencyStrings.kt    # Strings multi-idioma
└── EmergencyUI.kt         # Interfaz Jetpack Compose
```

---

## 🚀 Modos de Orion

### ☁️ NUBE (Cloud)
- GPT-4, Claude, Gemini, DeepSeek, Qwen, Kimi
- Memoria persistente automática
- Máxima potencia

### 📱 LOCAL (Offline)
- Modelos GGUF optimizados para móvil
- Privacidad total
- Sin necesidad de Internet

### 👁️ VIVO (Realtime)
- OpenAI Realtime API
- Conversación por voz duplex
- Respuesta instantánea

### 🆘 EMERGENCIA (Mesh)
- WiFi Direct P2P
- Cifrado extremo a extremo
- Funciona sin Internet ni datos

---

## 📋 Requisitos

- Android 8.0+ (API 26)
- WiFi Direct compatible
- Permisos de ubicación (para WiFi Direct)

## 🔧 Permisos Necesarios (AndroidManifest.xml)

```xml
<!-- WiFi Direct -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

<uses-feature android:name="android.hardware.wifi.direct" android:required="false" />
```

## 🔧 Compilación

```bash
./gradlew assembleDebug
```

---

**¡Mantén a tu familia conectada incluso sin Internet!** 🆘📡
