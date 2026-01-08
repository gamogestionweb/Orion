# Emergency Mode Update - QR Scanner

## Modified/Added Files

1. `EmergencyUI.kt` - Enhanced UI
2. `PortraitCaptureActivity.kt` - Vertical scanner (NEW)
3. `AndroidManifest.xml` - Permissions and activity

## Dependencies (build.gradle.kts)

```kotlin
// ZXing - Generate and scan QR
implementation("com.google.zxing:core:3.5.2")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

## Changes in AndroidManifest.xml

Add these two elements:

### 1. Camera permission (in permissions section):
```xml
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

### 2. Scanner activity (inside <application>):
```xml
<activity
    android:name=".emergency.PortraitCaptureActivity"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.ProyectoOrion" />
```

## Improvements

### ✅ MAINTAINED (all original features):
- E2E encryption with ECDH + AES-GCM
- Public and private message sending
- Message decryption
- Colored message cards
- WiFi Direct Service Discovery
- Automatic mesh connection

### ✅ IMPROVED:
- **ACTIVATE button always visible** (previously disappeared)
- **My code with visual QR** (previously text only)
- **Visible contact list** (👥 icon in toolbar)
- **Vertical QR scanner** (previously rotated horizontal)
- **Clear icons**:
  - 🟢 QrCode2 = My QR (green, highlighted)
  - 👥 People = Family list
  - 📷 QrCodeScanner = Scan to add

### ✅ ADDED:
- Contact chips for quick selection
- Scan button inside add dialog
- Family list view with delete option
- Scanned QR code auto-fills

### ❌ REMOVED:
- Manual IP connect button (not needed)

## Usage Flow

### Share your code:
1. Tap green icon (QrCode2) → View your QR
2. Family member scans your QR
3. They can now send you encrypted messages

### Add family member:
1. Tap scanner icon (QrCodeScanner)
2. Scan family member's QR
3. Enter their name → Add
4. Done! You can now send them encrypted messages

### Send encrypted message:
1. Activate MESH
2. Select contact in chips
3. Check "Encrypted" ✓
4. Write and send
