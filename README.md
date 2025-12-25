# Manga Overlay App

An Android overlay app that floats over manga reader apps for Japanese OCR, furigana, and translation.

## Current Status: Phase 1 Complete ✅

Phase 1 implements the basic overlay infrastructure with a draggable floating button.

## Features (Phase 1)

- ✅ Floating overlay button that appears over other apps
- ✅ Draggable button - long press and move anywhere on screen
- ✅ Foreground service with persistent notification
- ✅ Overlay permission handling
- ✅ Service lifecycle management from main activity
- ✅ Material Design 3 UI

## Technology Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34
- **Architecture:** Service-based overlay
- **UI:** Material Design 3 + ViewBinding

## Project Structure

```
app/src/main/java/com/mangaoverlay/app/
├── MainActivity.kt              # Main activity with permission handling
├── OverlayService.kt            # Foreground service managing the overlay
└── utils/
    └── PermissionHelper.kt      # Helper for overlay permissions

app/src/main/res/
├── layout/
│   ├── activity_main.xml        # Main screen UI
│   └── overlay_button.xml       # Floating button layout
├── drawable/
│   └── ic_translate.xml         # Translation icon
└── values/
    ├── colors.xml               # Material 3 colors
    └── strings.xml              # App strings
```

## How to Build

### Prerequisites

- Android Studio (latest stable version)
- Android SDK with API level 26+
- Physical Android device (recommended for testing overlays)

### Build Steps

1. **Clone the repository:**
   ```powershell
   cd C:\Users\Crimson\Desktop\languageHelperApp
   ```

2. **Build the project:**
   ```powershell
   .\gradlew assembleDebug
   ```

3. **Install on device:**
   ```powershell
   .\gradlew installDebug
   ```

   Or use Android Studio:
   - Open project in Android Studio
   - Click "Run" (Shift+F10)

## How to Test the Overlay

### Initial Setup

1. Launch the app
2. Tap "Enable Overlay" button
3. You'll be taken to Android settings
4. Toggle "Allow display over other apps" ON
5. Return to the app (it will auto-start the overlay)

### Testing the Floating Button

1. **Overlay appears:** A circular floating button should appear in the top-right corner
2. **Dragging:** Long press and drag the button anywhere on the screen
3. **Clicking:** Tap the button - should show "Capture feature coming soon!"
4. **Opacity:** Button is semi-transparent (80%) by default, becomes fully opaque when touched
5. **Persistence:** Press Home button - overlay should remain visible over other apps
6. **Notification:** A persistent notification "Manga Overlay Active" should be visible

### Testing Service Management

1. **Stop overlay:** Return to app and tap "Stop Overlay" button
2. **Restart overlay:** Tap "Start Overlay" to restart the service
3. **App closure:** Close app completely - overlay should continue running
4. **System restart:** Service uses START_STICKY, so it will attempt to restart if killed

### Testing on Other Apps

1. Start the overlay service
2. Open any manga reader app or browser
3. The floating button should appear on top
4. Test dragging and clicking while other apps are active

## Current Limitations (Phase 1)

- ❌ No screenshot capture (coming in Phase 2)
- ❌ No OCR functionality (coming in Phase 3)
- ❌ No translation API integration (coming in Phase 3)
- ❌ Button click only shows a toast message
- ⚠️ Overlays may behave differently on emulators - **test on a real device**

## Known Issues

- On some devices, the overlay permission setting might be in a different location
- Service detection using `getRunningServices()` is deprecated but functional for this use case
- Button positioning uses screen coordinates - may need adjustment for different screen sizes

## Permissions

The app requires the following permissions:

- `SYSTEM_ALERT_WINDOW` - Display floating overlay over other apps
- `INTERNET` - For future API calls (Phase 3)
- `FOREGROUND_SERVICE` - Run overlay service in foreground
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - For screenshot capture (Phase 2)
- `POST_NOTIFICATIONS` - Show foreground service notification

## Development Commands

```powershell
# Build debug APK
.\gradlew assembleDebug

# Run unit tests
.\gradlew test

# Run instrumented tests (requires device)
.\gradlew connectedAndroidTest

# Clean build
.\gradlew clean

# Lint checks
.\gradlew lint
```

## Next Steps (Upcoming Phases)

**Phase 2:** Screenshot capture
- Implement MediaProjection for screen capture
- Capture screen when button is clicked
- Display captured image in overlay

**Phase 3:** OCR and Translation
- Integrate Japanese OCR API
- Add furigana support
- Implement translation API
- Display results in overlay

## Architecture Notes

### Service Lifecycle

- `OverlayService` runs as a foreground service with ongoing notification
- Service starts with `START_STICKY` - system will attempt to restart if killed
- WindowManager manages the overlay view lifecycle
- Service destroys overlay view when stopped

### Touch Handling

- `OnTouchListener` distinguishes between clicks and drags
- Movement threshold: 10px - below this is considered a click
- Initial position stored on ACTION_DOWN
- Position updated on ACTION_MOVE if threshold exceeded
- ACTION_UP triggers click if not moved

### Permission Flow

1. App checks for `SYSTEM_ALERT_WINDOW` permission
2. If not granted, shows "Enable Overlay" button
3. Button launches system settings via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
4. Activity result launcher checks permission on return
5. If granted, service starts automatically

## Troubleshooting

**Overlay doesn't appear:**
- Check if overlay permission is granted in Settings > Apps > Manga Overlay > Display over other apps
- Verify service is running (notification should be visible)
- Check Logcat for errors

**Button not draggable:**
- Ensure you're long-pressing, not just tapping
- Try moving more than 10px (the movement threshold)

**Service stops unexpectedly:**
- Check battery optimization settings - disable for this app
- Some manufacturers aggressively kill background services - check device-specific settings

## License

This project is for development purposes.

---

**Current Version:** 1.0 (Phase 1)
**Last Updated:** 2025-11-20
