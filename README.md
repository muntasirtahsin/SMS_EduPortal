# EduPortal

EduPortal is a JavaFX student management system. Releases are published through GitHub Actions and delivered to installed Windows clients by the in-app updater.

## Publishing an update

1. Make and push the application changes to `main`.
2. Set `CURRENT_VERSION` in `src/com/eduportal/utils/AutoUpdater.java` to the next version, for example `v1.0.1`, and push that change.
3. Create and push a matching tag:

   ```powershell
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. The **Publish EduPortal release** workflow builds `EduPortal-update.zip` and attaches it to a GitHub Release. The app checks the latest release on startup and installs that asset when a newer version is available.

The tag must exactly match `CURRENT_VERSION`. The repository must be public for clients to check and download releases without GitHub authentication. The update ZIP is separate from the Launch4j EXE; build the initial EXE with `dist/EduPortal/launch4j.xml`.

Email OTP requires `GMAIL_USER` and `GMAIL_PASS` environment variables, or those values in a local `.env` file. Do not commit credentials.

## Build locally on Windows

Install JDK 17 or newer, then run:

```powershell
./download_deps.ps1
./prepare-launch4j.ps1
```

The script writes the distributable package and update ZIP under `dist/EduPortal`. `config.properties` is local-only; `config.properties.example` is the safe default template.
