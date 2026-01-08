# Dependencies for QR Scanner - UPDATED

## Gradle (app/build.gradle.kts)

**REMOVE** (if present):
```kotlin
// DELETE THIS:
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

**ADD** these dependencies:

```kotlin
// ZXing for QR GENERATION
implementation("com.google.zxing:core:3.5.2")

// ZXing Embedded for QR SCANNING (includes camera Activity)
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

## Complete CameraX dependencies (if not present):

```kotlin
// CameraX (optional, ZXing Embedded already handles camera)
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

## Summary of Changes

### ❌ REMOVED:
- ML Kit Barcode Scanner (caused resolution issues)
- Custom scanner with CameraX (complex)
- Manual IP connect button

### ✅ ADDED:
- ZXing Embedded (`journeyapps`) - Proven library, works offline
- Visual QR generated with ZXing Core
- Simplified flow: Scan → Name → Done
- Contact chips for quick selection

### Advantages of ZXing Embedded:
1. **Own Activity** - No need to manage CameraX
2. **Offline** - Doesn't require Google Play Services
3. **Proven** - Millions of downloads, very stable
4. **Simple** - One launcher and ready

## Code Usage

```kotlin
// Create launcher
val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
    result.contents?.let { code -> /* scanned code */ }
}

// Launch scanner
val options = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt("Scan QR code")
    setBeepEnabled(false)
}
qrScannerLauncher.launch(options)
```
