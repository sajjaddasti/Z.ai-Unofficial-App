# z.ai Unofficial — native Android client

A small, polished Android shell for [z.ai](https://chat.z.ai) built with Kotlin and
Jetpack Compose. You sign in on the real z.ai website inside a private WebView —
no API key, no scraping, no token hacking. SSO, 2FA and CAPTCHA all work because
the login *is* the website. Session cookies persist across restarts.

## Highlights

- **2026-era UI** — floating glass header, mint→aqua gradient accents, rounded
  cards, animated loading state and a proper bottom sheet instead of a drawer.
- **Theme control** — Auto / Light / Dark, remembered between launches.
- **Edge-to-edge** — content flows under the system bars with correct insets.
- **Session aware** — cookies are flushed on pause, kept across restarts, and can
  be cleared from Settings.
- **Resilient** — offline screen, HTTP/error screen, automatic recovery when the
  renderer process dies, and a compatibility screen only when the engine is
  genuinely old. A modern engine that paints nothing gets a dismissible
  "page looks empty" card (reload / open in browser) instead of a blocking
  accusation.
- **Escape hatches** — one-tap **Desktop site** mode (Chrome UA + desktop layout)
  from the menu or Settings if the mobile page ever misbehaves.
- **Native plumbing** — file uploads, camera/mic permissions, JS dialogs, popups
  (OAuth windows are routed back into the app), downloads, external links,
  Android app shortcuts, optional biometric lock.
- **Diagnostics** — live WebView package/version, UA, paint-probe result, a
  built-in JS console and a **Copy diagnostics** button for bug reports.

## Requirements

- Android 8.0 (API 26) or newer.
- **Android System WebView 115+** is recommended. z.ai uses modern CSS that older
  Chromium engines (for example the AOSP WebView 110 shipped in some emulators
  and old ROMs) cannot lay out — the page loads but paints nothing. The app shows
  the Play Store update path only when the detected engine is actually below
  Chromium 115; current engines are never blamed for a slow page.
- If the mobile layout ever stays blank on a current phone, enable **Desktop
  site**, then send the output of **Settings → Diagnostics → Copy diagnostics**.

## Install

1. Grab `zai-unofficial-<version>.apk` from the latest GitHub Actions artifact or
   Release.
2. Sideload it and open the app, then log in normally on the z.ai page.

## Build

```bash
# JDK 17 + Android SDK 34 required
./gradlew assembleRelease      # signed with the debug key, ready to sideload
./gradlew assembleDebug
```

CI (`.github/workflows/build.yml`) builds both variants on every push to `main`
and uploads them as the `zai-unofficial-apk` artifact.

## Why updating WebView from the Play Store sometimes does nothing

Android chooses the WebView provider from the system image, not from whatever APK
you install. On many emulators (and some ROMs) the provider is the AOSP stub
`com.android.webview`, which cannot be replaced by the Play Store package
`com.google.android.webview` — the install is ignored or fails with a signature
mismatch. On a normal retail phone the Play Store version *is* the provider, so
updating "Android System WebView" (and Chrome) fixes rendering after a restart.
Settings → Diagnostics always shows the engine package and version actually in
use.

## Privacy

- Login happens on `chat.z.ai`; credentials are never seen by this app.
- Cookies live in the WebView's private storage on your device.
- No analytics, no ads, no third-party SDKs.

## Disclaimer

Unofficial project. Not affiliated with, endorsed by, or supported by z.ai.
z.ai and GLM are trademarks of their respective owners. Use at your own risk.
