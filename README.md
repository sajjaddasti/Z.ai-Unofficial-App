# z.ai Unofficial (Android, native shell + WebView)

Native Kotlin + Jetpack Compose Android app that wraps the z.ai web app in a
WebView. No API key required — you log in through the real z.ai website inside
the app, so SSO/2FA/CAPTCHA all work. Session cookies persist across restarts.

## Open in Android Studio
1. File → Open → select this `zai-unofficial` folder.
2. Let Gradle sync.
3. Run on a device/emulator (minSdk 26).

## Features
- Material 3 with dynamic color (Material You)
- Edge-to-edge, immersive status bar
- Splash screen (Android 12+ API)
- Pull-to-refresh
- Native offline / error screen with retry
- Cookie/session persistence across restarts
- App shortcuts (long-press icon → "New chat")
- Biometric lock toggle (optional, in settings)

## Notes
- This is an unofficial client. Use at your own risk; z.ai may change their site.
- Rename/rebrand before any public distribution; do not use z.ai trademarks.
