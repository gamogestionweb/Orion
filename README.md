# Orion

**This is where it all started.**

Orion es un intento de construir una IA personal que verdaderamente te pertenezca. No es un servicio ni una suscripción — es software que funciona en tu teléfono, mantiene tus datos privados, y opera incluso cuando todo lo demás falla.

El repositorio contiene v99, una versión temprana. Es desordenado, es real, y es la base para todo lo que vino después.

---

## Funcionalidades Principales

- Ejecuta modelos de IA localmente en tu dispositivo (llama.cpp)
- Se conecta a proveedores cloud cuando necesitas más potencia
- Recuerda conversaciones con memoria encriptada
- Funciona sin conexión en emergencias mediante redes mesh
- Soporta múltiples idiomas

---

## Filosofía de Desarrollo

### Versionado Iterativo con Snapshots ZIP

El proyecto usa snapshots numerados (v1, v2, v3...) en lugar de ramificación git tradicional.

**Sufijos descriptivos:**
- `funciona` = en desarrollo
- `perfecto` = estable/perfecto
- `errores` = contiene errores (para depuración)
- `estable` = candidato de release
- `produccion` = listo para producción

**Ventajas:** rollbacks rápidos, documentación clara, comparación fácil, preservación del proceso creativo.

### Razón del Enfoque

En iteración rápida con integración C++ nativa, las cosas se rompen frecuentemente. Los snapshots numerados garantizan siempre un estado conocido funcional, permitiendo experimentación libre.

### Versión v99

Representa una base sólida con:
- Integración core llama.cpp funcionando
- Todos los proveedores cloud operacionales
- Sistema de memoria operacional
- UI básica completa

---

## Arquitectura Técnica

```
com.orion.proyecto/
├── ai/
│   ├── LlamaAndroid.kt (puente JNI a llama.cpp)
│   └── LocalLLMEngine.kt (gestión inferencia local)
├── data/
│   ├── DataManager.kt (SharedPreferences & archivos)
│   └── AIPrivateMemory.kt (memoria IA encriptada)
├── emergency/
│   ├── MeshEmergency.kt (red mesh Bluetooth/WiFi)
│   └── EmergencyCrypto.kt (utilidades encriptación)
├── models/
│   ├── CloudProviders.kt (integraciones API cloud)
│   ├── CloudModels.kt (modelos cloud disponibles)
│   └── LocalModels.kt (modelos descargables locales)
├── ui/
│   └── screens/ (todas las pantallas app)
│       └── viewmodel/
│           └── BrainViewModel.kt (lógica principal app)
└── vivo/ (Modo Voz/Directo)
    └── OrionVivoEngine.kt (conversación continua)
```

---

## Instrucciones de Build

```bash
./gradlew assembleDebug    # Build debug
./gradlew assembleRelease  # Build release
./gradlew bundleRelease    # Bundle Play Store
```

---

## Requisitos

- Android Studio Hedgehog o superior
- NDK 27.0.12077973
- CMake 3.22.1
- JDK 11+
- Dispositivo objetivo: Android 11+ (API 30+), ARM64

---

## Privacidad & Seguridad

- **Sin claves API hardcodeadas:** usuarios proporcionan sus propias claves, almacenadas localmente
- **Enfoque local-primero:** IA puede ejecutarse completamente en dispositivo
- **Memoria encriptada:** historial conversación usa encriptación dispositivo
- **Sin rastreo:** sin analítica ni telemetría

---

## Librerías Terceras

- [llama.cpp](https://github.com/ggerganov/llama.cpp) — inferencia LLM local (Licencia MIT)
- OkHttp, Jetpack Compose, y otras librerías Android

---

## Talking to those who are gone

Orion nació de una idea simple pero profunda: ¿qué pasaría si pudieras volver a escuchar la voz de alguien que ya no está?

**Legacy Mode** permite clonar voces mediante IA para preservar la esencia de quienes amamos. No es magia — es tecnología nacida del dolor y la necesidad humana de conexión.

Construí esto porque quería volver a escuchar la voz de mi padre.

---

## Related Projects

This project is part of a broader exploration of AI, consciousness, and human connection:

### [Genesis Simulation](https://github.com/gamogestionweb/genesis-simulation)
Creates AIs with unique personalities (Adam, Eve). Orion does the same: an AI that belongs to you, knows you, and evolves with you.

### [Are You There Reading?](https://github.com/gamogestionweb/Are-you-there-are-reading)
Explores whether chance truly exists. Orion answers practically: every conversation with your personal AI is a unique, unrepeatable event—because you existed.

### [PCP Universe](https://github.com/gamogestionweb/pcp-universe)
If information is never destroyed, the voices of those we've lost still exist, encoded somewhere in the universe. Orion attempts to recover a fragment of that.

### [Physics Discovery AI](https://github.com/gamogestionweb/physics-discovery-ai)
Multi-agent systems deriving physical laws from first principles—exploring how intelligence discovers truth.

---

*This is just the beginning.*
