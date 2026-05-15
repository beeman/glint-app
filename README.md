# Glint

Glint is a lightweight Android wallpaper source for Solana Mobile wallpapers.

The app fetches a remote wallpaper catalog, renders a Compose grid of preview
images, downloads the selected wallpaper into app-private cache, and hands the
cached PNG to Android's wallpaper crop/set flow.

## Wallpaper Catalog

Glint loads its catalog at runtime from:

```text
https://raw.githubusercontent.com/beeman/solana-mobile-wallpapers/main/wallpapers.json
```

The wallpaper images are not bundled into this app. The catalog and image assets
remain in the [`beeman/solana-mobile-wallpapers`](https://github.com/beeman/solana-mobile-wallpapers) repository.

Each catalog entry is expected to include:

- `imageUrl`
- `name`
- `previewUrl`
- `slug`

## Development

Open this repository in Android Studio, or use `just` from the repo root.

Run unit tests:

```bash
just test
```

Build and test the debug app:

```bash
just check
```

Build a debug APK:

```bash
just build-debug
```

Install the debug APK on a connected device or emulator:

```bash
just install-debug
```

Launch Glint directly:

```bash
just launch
```

Check whether Android discovers Glint as a wallpaper-setting activity:

```bash
just wallpaper-activities
```

## Release Builds

Create local signing material:

```bash
just setup-signer
```

This creates:

- `.signing/glint-release.jks`
- `signing.properties`

Both files are ignored by git. Keep them private. Losing them means future APKs
will be signed by a different key.

Build a signed release APK:

```bash
just release-signed
```

The signed release artifact is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## Current Behavior

- Fetches the live wallpaper manifest on app launch and Refresh.
- Shows a two-column Compose grid with remote preview thumbnails.
- Retries visible thumbnail loading on Refresh.
- Shows a wallpaper count when the catalog is loaded.
- Downloads the selected wallpaper PNG to app cache.
- Opens Android's crop/set wallpaper flow when available.
- Falls back to `WallpaperManager.setStream(...)` if the crop intent is not available.

## PoC Boundaries

This app intentionally does not:

- Bundle wallpaper PNGs.
- Provide a live wallpaper service.
- Register as a collection inside Pixel's system Wallpaper picker.
- Include accounts, analytics, search, filters, categories, or publishing automation.

Pixel's Wallpaper collections screen is owned by the system/Google wallpaper
picker. Glint is currently discoverable as an `ACTION_SET_WALLPAPER` activity,
but it is not expected to appear as a first-party collection tile there.

## Project Shape

- Android package: `dev.beeman.glint`
- Minimum SDK: 26
- Target SDK: 36
- UI: Kotlin + Jetpack Compose + Material 3
- Networking: platform `HttpURLConnection`
- Wallpaper handoff: platform `WallpaperManager`
- Cached wallpaper sharing: read-only `ContentProvider`
