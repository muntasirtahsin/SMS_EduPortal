# EduPortal

EduPortal is a JavaFX student management system. Releases are published through GitHub Actions and delivered to installed Windows clients by the in-app updater.

## Publishing an update

1. Push your changes to `main`.
2. In GitHub, open **Actions** → **Publish EduPortal release** → **Run workflow**.
3. The workflow automatically increments the patch version, builds `EduPortal-update.zip`, and publishes a GitHub Release. No version edits or manual ZIP uploads are needed.

The repository must be public so installed clients can check and download releases without GitHub authentication. The update ZIP is separate from the Launch4j EXE; build the initial EXE with `dist/EduPortal/launch4j.xml`.

Email OTP requires `GMAIL_USER` and `GMAIL_PASS` environment variables, or those values in a local `.env` file. Do not commit credentials.

## Build locally on Windows

Install JDK 17 or newer, then run:

```powershell
./download_deps.ps1
./prepare-launch4j.ps1
```

The script writes the distributable package and update ZIP under `dist/EduPortal`. `config.properties` is local-only; `config.properties.example` is the safe default template. Release builds embed the generated version in the JAR.
