# OneDrive to Google Photos Sync (Android)

A minimal foreground-service + WorkManager scaffold. It will later authenticate to OneDrive and Google to sync media and delete after upload.

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

## Next

- Wire MSAL (Azure App Registration), Microsoft Graph list of media
- Google Sign-In/OAuth and upload via Drive or Photos Library upload tokens
- Post-success deletes (OneDrive + device via SAF)