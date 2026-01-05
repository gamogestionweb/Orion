# Dependencias para QR Scanner - ACTUALIZADO

## Gradle (app/build.gradle.kts)

**QUITAR** (si lo tienes):
```kotlin
// BORRAR ESTO:
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

**AÑADIR** estas dependencias:

```kotlin
// ZXing para GENERAR QR
implementation("com.google.zxing:core:3.5.2")

// ZXing Embedded para ESCANEAR QR (incluye Activity de cámara)
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

## Dependencias completas de CameraX (si no las tienes):

```kotlin
// CameraX (opcional, ZXing Embedded ya maneja la cámara)
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

## Resumen de cambios

### ❌ ELIMINADO:
- ML Kit Barcode Scanner (causaba problemas de resolución)
- Scanner personalizado con CameraX (complejo)
- Botón de conectar IP manual

### ✅ AÑADIDO:
- ZXing Embedded (`journeyapps`) - Librería probada, funciona offline
- QR visual generado con ZXing Core
- Flujo simplificado: Escanear → Nombre → Listo
- Chips de contactos para selección rápida

### Ventajas de ZXing Embedded:
1. **Activity propia** - No necesitas manejar CameraX
2. **Offline** - No requiere Google Play Services
3. **Probado** - Millones de descargas, muy estable
4. **Simple** - Un launcher y listo

## Uso en código

```kotlin
// Crear launcher
val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
    result.contents?.let { code -> /* código escaneado */ }
}

// Lanzar scanner
val options = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt("Escanea el QR")
    setBeepEnabled(false)
}
qrScannerLauncher.launch(options)
```
