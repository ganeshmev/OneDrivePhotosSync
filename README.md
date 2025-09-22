# OneDrive to Google Photos Sync (Android)

A minimal foreground-service + WorkManager scaffold. It now includes OAuth scaffolding and placeholder clients for OneDrive (list/delete) and Google uploads. Next we’ll wire real OAuth and the full sync pipeline.

## Build locally (Windows)

1. Install Android Studio (SDK 34, Build-Tools 34.x, Platform Tools, cmdline-tools) and JDK 17.
2. Ensure `local.properties` exists with your SDK path, for example:

```
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

3. Build:

```
./gradlew assembleDebug -x lint
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Build via GitHub Actions

Push to `main` or trigger the workflow manually. The debug APK is uploaded as an artifact named `app-debug-apk`.

On tags matching `v*`, a GitHub Release is created and the APK(s) are attached. If signing secrets are configured, a signed `app-release.apk` is also attached.

## Usage

1. Install the APK on your Pixel.
2. Open the app and tap “Start Background Sync.” A foreground notification appears.
3. Use “Grant Permissions” if prompted. The app uses network access and, later, SAF for local file deletions.
4. Battery optimization: tap the button to exclude the app for more reliable background work.

Current behavior: The worker runs every ~15 minutes and executes placeholder logic. Once OAuth is configured, it will authenticate, list new OneDrive media, upload to Google, and delete on success.

## OAuth configuration (coming next)

We’ll use AppAuth (PKCE) for both providers.

- OneDrive (Azure AD)
	- Register an app in Azure Portal
	- Set redirect URI to an app scheme, e.g., `onedrivephotosync://auth`
	- Scopes: Files.Read, Files.ReadWrite, offline_access
	- Put client details in a small config and wire into `OneDriveAuth`

- Google
	- Create an OAuth client (Android) in Google Cloud Console
	- Configure SHA-1 for your signing key
	- Scopes: Drive (or Photos Library flow)
	- Wire into `GoogleAuth`

Tokens are stored with AndroidX Security Crypto (`EncryptedSharedPreferences`).

## Background execution

- Foreground service keeps the app alive with low-priority notification
- WorkManager schedules periodic sync (~15 minutes)
- For OEM battery optimizations, exclude the app for best reliability

## Signing

- Debug APKs are auto-signed by Gradle (free) and installable on device
- CI supports optional release signing via GitHub Secrets:
	- `SIGNING_KEYSTORE_B64` (base64 of your keystore.jks)
	- `SIGNING_STORE_PASSWORD`
	- `SIGNING_KEY_ALIAS`
	- `SIGNING_KEY_PASSWORD`
- On tags, if secrets are present, a signed `app-release.apk` is produced and attached to the Release

## Roadmap

- Implement real OneDrive listing via Microsoft Graph
- Implement Google upload (Drive or Photos upload token flow)
- Delete from OneDrive + local (SAF) after successful upload
- Progress UI, error handling, and logs

## Next

- Wire MSAL (Azure App Registration), Microsoft Graph list of media
- Google Sign-In/OAuth and upload via Drive or Photos Library upload tokens
- Post-success deletes (OneDrive + device via SAF)