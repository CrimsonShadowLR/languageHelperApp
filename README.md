# Manga Overlay App

> An Android overlay application that provides floating OCR and translation capabilities for Japanese manga readers, powered by Google's Gemini API.

## Overview

This Android overlay app floats over manga reader apps to capture, process, and translate Japanese text to Spanish. The app uses Google's Gemini API to return edited manga images with Spanish text overlaid directly on the panels.

**Current Status:** ✅ Fully Functional - Translation Pipeline Complete

---

<details>
<summary><strong>✨ Features</strong></summary>

### Core Functionality
- ✅ **Floating Overlay Button** - Appears over other apps with drag-and-drop positioning
- ✅ **Screenshot Capture** - MediaProjection-based screen capture with permission reuse
- ✅ **Interactive Cropping** - 8-handle crop interface (corners, edges, center drag)
- ✅ **AI Translation** - Gemini API integration for Japanese-to-Spanish manga translation
- ✅ **Image Processing** - Intelligent compression (target 75KB, max 100KB)
- ✅ **Rate Limiting** - Concurrent request management with exponential backoff retry
- ✅ **Image Saving** - Save translated images to device gallery with MediaStore
- ✅ **Foreground Service** - Persistent overlay with notification

### User Experience
- Material Design 3 UI with modern aesthetics
- Semi-transparent overlay (80% opacity, 100% on touch)
- Touch handling distinguishes clicks (<10px) from drags
- Rule of thirds grid overlay during cropping
- Loading indicators and error feedback
- Permission flow with automatic service startup

</details>

<details>
<summary><strong>🛠️ Technology Stack</strong></summary>

| Category | Technology |
|----------|-----------|
| **Language** | Kotlin |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 |
| **Architecture** | Service-based overlay with coroutines |
| **UI Framework** | Material Design 3 + ViewBinding + Jetpack Compose |
| **Networking** | Retrofit + OkHttp |
| **Async** | Kotlin Coroutines + Flow |
| **Image Processing** | Android Bitmap + ImageReader |
| **AI API** | Google Gemini API (generative-language) |

</details>

<details>
<summary><strong>📁 Project Structure</strong></summary>

```
com.mangaoverlay.app/
├── api/
│   ├── GeminiApiService.kt          # Retrofit interface for Gemini API
│   ├── TranslationClient.kt         # Rate limiting, retry logic, response handling
│   ├── TranslationRequest.kt        # Request models for Gemini API
│   └── TranslationResponse.kt       # Response models (handles snake_case & camelCase)
│
├── ui/
│   ├── CropView.kt                  # Custom crop view with 8 draggable handles
│   └── LoadingDialog.kt             # Translation progress dialog
│
├── utils/
│   ├── ImageProcessor.kt            # Two-pass compression (prescale + binary search)
│   ├── PermissionHelper.kt          # Overlay permission management
│   └── ScreenCaptureManager.kt      # MediaProjection lifecycle & VirtualDisplay
│
├── MainActivity.kt                  # Entry point, permission handling
├── OverlayService.kt                # Foreground service for floating button
└── CropActivity.kt                  # Full-screen cropping & translation UI

res/
├── layout/
│   ├── activity_main.xml            # Main screen with overlay controls
│   ├── activity_crop.xml            # Crop screen layout
│   └── overlay_button.xml           # Floating button design
├── drawable/
│   └── ic_translate.xml             # Material icon assets
└── values/
    ├── colors.xml                   # Material 3 color scheme
    └── strings.xml                  # Localized strings
```

</details>

<details>
<summary><strong>🚀 Getting Started</strong></summary>

### Prerequisites

- **Android Studio** (latest stable version recommended)
- **Android SDK** with API level 26+
- **Physical Android device** (overlays behave differently on emulators)
- **Gemini API Key** ([Get one here](https://ai.google.dev/))

### API Configuration

Create a `local.properties` file in the project root (this file is gitignored):

```properties
GEMINI_API_KEY=your_api_key_here
```

The API key is accessed via `BuildConfig.GEMINI_API_KEY` at runtime.

### Build Commands

```powershell
# Build debug APK
.\gradlew assembleDebug --stacktrace

# Install on connected device
.\gradlew installDebug

# Run unit tests
.\gradlew test

# Run instrumented tests (requires device)
.\gradlew connectedAndroidTest

# Clean build
.\gradlew clean

# Lint checks
.\gradlew lint
```

### Quick Start

1. **Clone the repository**
2. **Add your Gemini API key** to `local.properties`
3. **Build and install:**
   ```powershell
   .\gradlew installDebug
   ```
4. **Grant overlay permission** when prompted
5. **Open a manga reader app** and tap the floating button

</details>

<details>
<summary><strong>🧪 Testing Guide</strong></summary>

### Initial Setup

1. **Launch the app** - Main screen appears
2. **Tap "Enable Overlay"** - Redirects to Android settings
3. **Toggle "Allow display over other apps" ON**
4. **Return to app** - Overlay service starts automatically
5. **Verify floating button** appears in top-right corner

### Testing Screenshot Capture

1. **Open a manga reader app** or any app with Japanese text
2. **Tap the floating button** - CropActivity launches with screenshot
3. **MediaProjection permission** should only prompt once, then persist
4. **Subsequent captures** should not reprompt for permission

### Testing Crop Interface

1. **8 draggable handles:**
   - 4 corner handles for aspect ratio adjustment
   - 4 edge midpoint handles for side-specific adjustments
   - Center area for moving entire crop region
2. **Touch detection:** 80px proximity threshold
3. **Coordinate mapping:** View coordinates → bitmap coordinates
4. **Rule of thirds grid** displays during interaction

### Testing Translation Pipeline

1. **Crop the desired manga panel**
2. **Tap "Translate" button**
3. **Loading dialog** shows progress
4. **Rate limiting:** Max 3 concurrent requests
5. **Retry logic:** 3 attempts with exponential backoff (1s, 4s, 9s)
6. **Image compression:** Target 75KB, max 100KB
7. **Result:** Edited manga image with Spanish text overlaid

### Testing Image Saving

1. **After translation completes**, tap the save button
2. **Grant storage permission** if prompted
3. **Image saved to** Pictures/MangaOverlay/ directory
4. **Media scan triggered** for gallery visibility
5. **Toast confirmation** shows save location

### Testing Service Lifecycle

- **Persistence:** Press Home - overlay remains visible
- **Notification:** "Manga Overlay Active" in status bar
- **Stop/Start:** Toggle overlay from main activity
- **App closure:** Overlay continues running (foreground service)
- **System restart:** Service uses START_STICKY for auto-restart

### Device Compatibility Testing

Test on various screen sizes and Android versions:
- Different DPI settings (coordinate mapping)
- Battery optimization settings (service persistence)
- Manufacturer-specific overlay restrictions
- Android 8.0 (SDK 26) through Android 14+ (SDK 36)

</details>

<details>
<summary><strong>🔐 Permissions</strong></summary>

### Required Permissions

| Permission | Usage | Request Type |
|------------|-------|--------------|
| `SYSTEM_ALERT_WINDOW` | Display floating overlay over other apps | Runtime (Settings) |
| `FOREGROUND_SERVICE` | Run overlay service in foreground | Manifest |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screenshot capture capability | Manifest |
| `POST_NOTIFICATIONS` | Show foreground service notification | Runtime (Android 13+) |
| `INTERNET` | Gemini API network requests | Manifest |
| `WRITE_EXTERNAL_STORAGE` | Save images (Android <10) | Runtime |
| `READ_MEDIA_IMAGES` | Media access (Android 13+) | Runtime |

### Permission Flow

```
App Launch → Check SYSTEM_ALERT_WINDOW → If not granted → Show "Enable Overlay" button
                     ↓
          Launch Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                     ↓
          User grants permission → ActivityResultLauncher detects
                     ↓
          Auto-start OverlayService → Floating button appears
```

</details>

<details>
<summary><strong>🏗️ Architecture Details</strong></summary>

### Service-Based Overlay System

**OverlayService** manages the floating button lifecycle:
- Runs as foreground service with `START_STICKY` for auto-restart
- Uses WindowManager to display overlay view above all apps
- Internal state tracking prevents multiple service instances
- Touch handling with click/drag differentiation (10px threshold)

### MediaProjection Lifecycle Management

**ScreenCaptureManager** optimizes permission handling:
- MediaProjection initialized once and stored for reuse
- VirtualDisplay created once per MediaProjection instance
- ImageReader with RGBA_8888 format captures screen as Bitmap
- **Critical:** Don't stop MediaProjection between captures to avoid reprompting

### Translation Pipeline (Three-Stage Processing)

#### 1. Image Compression (ImageProcessor)
```
Input Bitmap → Prescale if >1920px → Binary search for optimal quality
              ↓
Target: 75KB, Max: 100KB, Quality: 40-85%
              ↓
JPEG compressed output
```

#### 2. API Request Management (TranslationClient)
```
Rate Limiting: Semaphore (3 concurrent max) + 1s minimum interval
              ↓
Retry Logic: 3 attempts with exponential backoff
              ↓
Request Format: generationConfig.responseModalities: ["TEXT", "IMAGE"]
```

#### 3. Response Processing
- API returns parts array with `inlineData` (image) and optional `text`
- Handles both snake_case (`inline_data`) and camelCase (`inlineDataCamelCase`)
- Base64 decode image data to Bitmap for display
- Returns edited manga image with Spanish text overlaid

### CropView Component Architecture

**Interactive cropping with 8 control points:**
- 4 corner handles: Maintain aspect ratio while scaling
- 4 edge midpoints: Adjust individual sides
- Center area: Move entire crop region
- 80px touch proximity threshold for handle detection
- Coordinate mapping: View space ↔ Bitmap space
- `setShowCropUI(false)` hides controls for clean image display

### Error Handling Strategy

**TranslationClient typed errors:**
- `ApiKeyNotConfigured` / `InvalidResponse` → Fail immediately, no retry
- `RateLimitExceeded` → Retry with exponential backoff
- `NetworkError` / `Timeout` → Retry with exponential backoff (max 3 attempts)

</details>

<details>
<summary><strong>🐛 Troubleshooting</strong></summary>

### Overlay Issues

**Overlay doesn't appear:**
- ✓ Check Settings > Apps > Manga Overlay > Display over other apps (enabled)
- ✓ Verify notification is visible ("Manga Overlay Active")
- ✓ Check Logcat for errors: `adb logcat | grep MangaOverlay`
- ✓ Restart app and service

**Button not draggable:**
- ✓ Ensure movement exceeds 10px threshold (not just vibration/jitter)
- ✓ Try intentional drag motion vs. quick tap
- ✓ Check touch event logging in Logcat

### Screenshot Issues

**Permission reprompts every time:**
- ✓ Ensure MediaProjection is NOT stopped between captures
- ✓ Check ScreenCaptureManager stores MediaProjection instance
- ✓ VirtualDisplay should persist across captures

**Screenshot is blank/black:**
- ✓ Test on physical device (emulators have limitations)
- ✓ Some apps block MediaProjection (DRM content)
- ✓ Check ImageReader format is RGBA_8888

### Translation Issues

**API errors:**
- ✓ Verify `GEMINI_API_KEY` in `local.properties`
- ✓ Check API key is valid at [Google AI Studio](https://ai.google.dev/)
- ✓ Review API quota limits (free tier restrictions)
- ✓ Check Logcat for specific error messages

**Image too large error:**
- ✓ ImageProcessor should compress to <100KB automatically
- ✓ Check compression parameters (quality 40-85%)
- ✓ Verify binary search logic in ImageProcessor.kt:42-68

**Rate limiting:**
- ✓ Max 3 concurrent requests enforced by semaphore
- ✓ 1-second minimum interval between requests
- ✓ Exponential backoff on rate limit errors

### Service Issues

**Service stops unexpectedly:**
- ✓ Disable battery optimization: Settings > Apps > Manga Overlay > Battery > Unrestricted
- ✓ Check manufacturer-specific restrictions (Xiaomi, Huawei, Samsung)
- ✓ Verify foreground service notification is not dismissed

**Service won't start:**
- ✓ Check Android version compatibility (SDK 26-36)
- ✓ Verify FOREGROUND_SERVICE permission in manifest
- ✓ Check for conflicting overlay apps

### Image Saving Issues

**Save fails silently:**
- ✓ Grant storage permissions (WRITE_EXTERNAL_STORAGE for Android <10)
- ✓ Check MediaStore API compatibility (Android 10+)
- ✓ Verify Pictures/MangaOverlay/ directory creation
- ✓ Check available storage space

**Image not visible in gallery:**
- ✓ Media scan should trigger automatically
- ✓ Manual scan: Settings > Storage > Scan media
- ✓ Check file actually exists: `adb shell ls /sdcard/Pictures/MangaOverlay/`

</details>

<details>
<summary><strong>💡 Development Notes</strong></summary>

### Key Implementation Details

**Response Field Name Handling:**
```kotlin
// API can return either snake_case or camelCase
val imageData = part.inlineData ?: part.inlineDataCamelCase
```

**Touch Event Logic:**
```kotlin
// Click vs. Drag distinction
ACTION_DOWN: Store initial position
ACTION_MOVE: If moved >10px, it's a drag
ACTION_UP: If not moved, it's a click
```

**Service State Management:**
```kotlin
// Internal state prevents multiple instances
companion object {
    var isRunning = false
}
```

**Compression Strategy:**
```kotlin
// Two-pass optimization
1. If width/height > 1920px → prescale to 1920px
2. Binary search quality (40-85%) until <75KB or <100KB max
```

### Testing Best Practices

- **Always test on physical devices** - emulator overlay behavior differs significantly
- **Test with real manga apps** - verify overlay appears correctly over target apps
- **Monitor API quota** - Gemini API has rate limits on free tier
- **Check multiple screen sizes** - coordinate mapping varies by DPI
- **Test permission persistence** - MediaProjection should not reprompt

### Common Pitfalls

❌ **Don't:** Stop MediaProjection after each capture
✅ **Do:** Store and reuse MediaProjection instance

❌ **Don't:** Assume response field names are always camelCase
✅ **Do:** Handle both snake_case and camelCase variations

❌ **Don't:** Create VirtualDisplay per capture
✅ **Do:** Create once per MediaProjection lifecycle

❌ **Don't:** Block UI thread during compression
✅ **Do:** Use coroutines for image processing

</details>

<details>
<summary><strong>📋 Development Commands</strong></summary>

### Build Commands

```powershell
# Build debug APK with full stacktrace
.\gradlew assembleDebug --stacktrace

# Build release APK (requires signing config)
.\gradlew assembleRelease

# Install on connected device
.\gradlew installDebug

# Uninstall from device
.\gradlew uninstallDebug
```

### Testing Commands

```powershell
# Run all unit tests
.\gradlew test

# Run unit tests with coverage
.\gradlew testDebugUnitTest --tests "*"

# Run instrumented tests (requires connected device)
.\gradlew connectedAndroidTest

# Run specific test class
.\gradlew test --tests "com.mangaoverlay.app.utils.ImageProcessorTest"
```

### Code Quality Commands

```powershell
# Run lint checks
.\gradlew lint

# Generate lint report
.\gradlew lintDebug

# Format code (if ktlint configured)
.\gradlew ktlintFormat

# Check code style
.\gradlew ktlintCheck
```

### Utility Commands

```powershell
# Clean build directory
.\gradlew clean

# List all tasks
.\gradlew tasks

# Dependency tree
.\gradlew app:dependencies

# Check for dependency updates
.\gradlew dependencyUpdates
```

### ADB Commands

```powershell
# View logs filtered to app
adb logcat | grep MangaOverlay

# Clear app data
adb shell pm clear com.mangaoverlay.app

# Grant overlay permission via ADB
adb shell appops set com.mangaoverlay.app SYSTEM_ALERT_WINDOW allow

# Check service status
adb shell dumpsys activity services com.mangaoverlay.app

# Take screenshot
adb exec-out screencap -p > screenshot.png
```

</details>

<details>
<summary><strong>🚦 Known Limitations</strong></summary>

### Platform Limitations

- **Emulator compatibility:** Overlays and MediaProjection behave differently on emulators - always test on physical devices
- **DRM content:** Some apps block MediaProjection for copyright protection
- **Manufacturer restrictions:** Some OEMs (Xiaomi, Huawei) have aggressive battery optimization that can kill foreground services
- **Android version variations:** Overlay permission location varies by manufacturer's settings app

### API Limitations

- **Gemini API quota:** Free tier has rate limits and daily quotas
- **Image size constraints:** API enforces maximum image size (hence aggressive compression)
- **Response time variability:** Translation can take 2-15 seconds depending on API load
- **Field name inconsistency:** API sometimes returns snake_case, sometimes camelCase

### Feature Limitations

- **Single language pair:** Currently hardcoded for Japanese → Spanish
- **No text-only mode:** Always processes and returns full image
- **No offline mode:** Requires active internet connection
- **No translation history:** Translations are not saved or logged
- **Button positioning:** Uses absolute screen coordinates, may need adjustment on edge cases

### Performance Considerations

- **Large images:** >1920px images are prescaled before compression
- **Memory usage:** Bitmap processing can be memory-intensive on older devices
- **Battery impact:** Foreground service with overlay has minimal but measurable battery drain

</details>

<details>
<summary><strong>📜 License</strong></summary>

This project is for educational and development purposes.

**Third-Party Services:**
- Google Gemini API - [Google AI Terms of Service](https://ai.google.dev/terms)
- Material Design - [Apache License 2.0](https://github.com/material-components/material-components-android/blob/master/LICENSE)

</details>

---

<div align="center">

**Version:** 2.0 (Translation Pipeline Complete)
**Last Updated:** 2025-12-25
**Min SDK:** 26 (Android 8.0) | **Target SDK:** 36

Made with ❤️ for manga readers

</div>
