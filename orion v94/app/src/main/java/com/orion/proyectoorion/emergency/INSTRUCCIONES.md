# Actualización Modo Emergencia - QR Scanner

## Archivos modificados/añadidos

1. `EmergencyUI.kt` - UI mejorada
2. `PortraitCaptureActivity.kt` - Scanner vertical (NUEVO)
3. `AndroidManifest.xml` - Permisos y activity

## Dependencias (build.gradle.kts)

```kotlin
// ZXing - Generar y escanear QR
implementation("com.google.zxing:core:3.5.2")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

## Cambios en AndroidManifest.xml

Añade estos dos elementos:

### 1. Permiso de cámara (en la sección de permisos):
```xml
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature 
    android:name="android.hardware.camera" 
    android:required="false" />
```

### 2. Activity del scanner (dentro de <application>):
```xml
<activity
    android:name=".emergency.PortraitCaptureActivity"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.ProyectoOrion" />
```

## Qué se ha mejorado

### ✅ MANTENIDO (todo lo original):
- Encriptación E2E con ECDH + AES-GCM
- Envío de mensajes públicos y privados
- Descifrado de mensajes
- Card de mensajes con colores
- WiFi Direct Service Discovery
- Conexión mesh automática

### ✅ MEJORADO:
- **Botón ACTIVAR siempre visible** (antes desaparecía)
- **Mi código con QR visual** (antes solo texto)
- **Lista de contactos visible** (icono 👥 en toolbar)
- **Scanner QR vertical** (antes giraba horizontal)
- **Iconos claros**:
  - 🟢 QrCode2 = Mi QR (verde, destacado)
  - 👥 People = Lista de familia
  - 📷 QrCodeScanner = Escanear para añadir

### ✅ AÑADIDO:
- Chips de contactos para selección rápida
- Botón de escanear dentro del diálogo de añadir
- Vista de lista de familia con opción de eliminar
- Código QR escaneado se auto-rellena

### ❌ ELIMINADO:
- Botón de conectar IP manual (no necesario)

## Flujo de uso

### Compartir tu código:
1. Toca el icono verde (QrCode2) → Ve tu QR
2. Tu familiar escanea tu QR
3. Ya te puede enviar mensajes cifrados

### Añadir familiar:
1. Toca el icono de scanner (QrCodeScanner)
2. Escanea el QR de tu familiar
3. Pon su nombre → Añadir
4. ¡Listo! Ya puedes enviarle mensajes cifrados

### Enviar mensaje cifrado:
1. Activa MESH
2. Selecciona contacto en los chips
3. Marca "Cifrado" ✓
4. Escribe y envía
