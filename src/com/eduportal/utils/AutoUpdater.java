package com.eduportal.utils;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AutoUpdater {

    public static final String CURRENT_VERSION = loadCurrentVersion();
    private static final String GITHUB_REPO = "muntasirtahsin/SMS_EduPortal";
    private static final String RELEASES_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final long MAX_UPDATE_SIZE = 1024L * 1024L * 1024L;

    private AutoUpdater() {}

    private static String loadCurrentVersion() {
        String fallback = "v1.0.0";
        try (InputStream input = AutoUpdater.class.getResourceAsStream("/version.properties")) {
            if (input == null) return fallback;
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("version", fallback).trim();
            if (!version.matches("[vV]?\\d+(\\.\\d+){1,3}")) {
                throw new IllegalStateException("Invalid application version in version.properties.");
            }
            return version.startsWith("v") ? version : "v" + version;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public enum State {
        CHECKING,
        CURRENT,
        UPDATE_AVAILABLE,
        UPDATE_BLOCKED,
        DOWNLOADING,
        INSTALLING,
        FAILED
    }

    public static final class UpdateStatus {
        private final State state;
        private final String message;
        private final String latestVersion;
        private final String downloadUrl;
        private final String sha256;

        private UpdateStatus(State state, String message, String latestVersion, String downloadUrl, String sha256) {
            this.state = state;
            this.message = message;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
        }

        public State getState() { return state; }
        public String getMessage() { return message; }
        public String getLatestVersion() { return latestVersion; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getSha256() { return sha256; }
    }

    public static void checkForUpdatesAsync(Consumer<UpdateStatus> listener) {
        publish(listener, status(State.CHECKING, "Checking for updates...", null, null, null));
        Thread updaterThread = new Thread(() -> {
            try {
                HttpURLConnection connection = openConnection(new URL(RELEASES_API));
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
                String json;
                int responseCode;
                try {
                    responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == 429) {
                        checkReleaseFeed(listener);
                        return;
                    }
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("GitHub returned HTTP " + responseCode + ".");
                    }
                    json = readResponse(connection);
                } finally {
                    connection.disconnect();
                }

                String latestVersion = jsonStringField(json, "tag_name");
                if (latestVersion == null || versionCompare(latestVersion, CURRENT_VERSION) == null) {
                    throw new IOException("The latest GitHub release has an invalid version tag.");
                }
                if (versionCompare(latestVersion, CURRENT_VERSION) <= 0) {
                    publish(listener, status(State.CURRENT,
                            "You're using the latest version (" + CURRENT_VERSION + ").", latestVersion, null, null));
                    return;
                }

                ReleaseAsset asset = findUpdateAsset(json);
                if (asset == null) {
                    publish(listener, status(State.UPDATE_BLOCKED,
                            "Version " + latestVersion + " is required, but its EduPortal-update.zip release asset is missing.",
                            latestVersion, null, null));
                    return;
                }

                publish(listener, status(State.UPDATE_AVAILABLE,
                        "Version " + latestVersion + " is required before you can sign in.",
                        latestVersion, asset.downloadUrl, asset.sha256));
            } catch (Exception e) {
                System.err.println("[AutoUpdater] Update check failed: " + e.getMessage());
                publish(listener, status(State.FAILED,
                        "Could not check for updates. Check your internet connection; you may continue with "
                                + CURRENT_VERSION + ".",
                        null, null, null));
            }
        }, "eduportal-update-check");
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    private static void checkReleaseFeed(Consumer<UpdateStatus> listener) throws IOException {
        URL feedUrl = new URL("https://github.com/" + GITHUB_REPO + "/releases.atom");
        HttpURLConnection connection = openConnection(feedUrl);
        connection.setRequestProperty("Accept", "application/atom+xml");
        String feed;
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub release feed returned HTTP " + responseCode + ".");
            }
            feed = readResponse(connection);
        } finally {
            connection.disconnect();
        }

        Matcher release = Pattern.compile("<entry\\b[^>]*>.*?<title>([^<]+)</title>", Pattern.DOTALL)
                .matcher(feed);
        if (!release.find()) throw new IOException("The GitHub release feed contains no published releases.");
        String latestVersion = release.group(1).trim();
        if (versionCompare(latestVersion, CURRENT_VERSION) == null) {
            throw new IOException("The latest GitHub release has an invalid version tag.");
        }
        if (versionCompare(latestVersion, CURRENT_VERSION) <= 0) {
            publish(listener, status(State.CURRENT,
                    "You're using the latest version (" + CURRENT_VERSION + ").", latestVersion, null, null));
            return;
        }

        String downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/"
                + latestVersion + "/EduPortal-update.zip";
        publish(listener, status(State.UPDATE_AVAILABLE,
                "Version " + latestVersion + " is required before you can sign in.",
                latestVersion, downloadUrl, null));
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    public static void installUpdateAsync(UpdateStatus update, Path applicationDirectory,
                                          Consumer<UpdateStatus> listener) {
        if (update == null || update.state != State.UPDATE_AVAILABLE || update.downloadUrl == null) {
            publish(listener, status(State.UPDATE_BLOCKED,
                    "This release does not have a downloadable update package.", update == null ? null : update.latestVersion,
                    null, null));
            return;
        }

        Thread installThread = new Thread(() -> {
            Path stageDirectory = null;
            Path updateZip = null;
            try {
                if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
                    throw new IOException("Automatic installation is currently supported on Windows only.");
                }
                Path appDirectory = applicationDirectory.toAbsolutePath().normalize();
                validateApplicationDirectory(appDirectory);
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData == null || localAppData.isBlank()) {
                    throw new IOException("The Windows local application data folder is unavailable.");
                }
                Path updateDirectory = Path.of(localAppData, "EduPortal", "updates");
                Files.createDirectories(updateDirectory);
                stageDirectory = Files.createTempDirectory(updateDirectory, "staging-");
                updateZip = updateDirectory.resolve("EduPortal-update-" + System.currentTimeMillis() + ".zip");

                publish(listener, status(State.DOWNLOADING, "Downloading " + update.latestVersion + "...",
                        update.latestVersion, update.downloadUrl, update.sha256));
                download(update.downloadUrl, updateZip);
                if (update.sha256 != null && !update.sha256.isEmpty()) {
                    verifySha256(updateZip, update.sha256);
                }

                extractUpdate(updateZip, stageDirectory);
                validateUpdatePackage(stageDirectory);
                publish(listener, status(State.INSTALLING, "Preparing the update. EduPortal will restart automatically...",
                        update.latestVersion, update.downloadUrl, update.sha256));
                launchInstallerHelper(appDirectory, stageDirectory, updateZip);
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            } catch (Exception e) {
                System.err.println("[AutoUpdater] Update installation failed: " + e.getMessage());
                deleteTree(stageDirectory);
                deleteFile(updateZip);
                publish(listener, status(State.UPDATE_AVAILABLE,
                        "Update failed: " + e.getMessage() + " Select Update Now to retry.",
                        update.latestVersion, update.downloadUrl, update.sha256));
            }
        }, "eduportal-update-install");
        installThread.setDaemon(true);
        installThread.start();
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "EduPortal-DesktopApp");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static void download(String downloadUrl, Path destination) throws Exception {
        URL url = new URL(downloadUrl);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("The release asset must be served over HTTPS.");
        }
        HttpURLConnection connection = openConnection(url);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub asset download returned HTTP " + responseCode + ".");
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_UPDATE_SIZE) {
                throw new IOException("The update package is larger than 1 GB.");
            }
            try (InputStream input = connection.getInputStream();
                 java.io.OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[16384];
                long totalBytes = 0;
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_UPDATE_SIZE) {
                        throw new IOException("The update package is larger than 1 GB.");
                    }
                    output.write(buffer, 0, bytesRead);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void verifySha256(Path file, String expectedDigest) throws Exception {
        String expected = expectedDigest.startsWith("sha256:")
                ? expectedDigest.substring("sha256:".length()) : expectedDigest;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
        }
        StringBuilder actual = new StringBuilder();
        for (byte value : digest.digest()) actual.append(String.format("%02x", value & 0xff));
        if (!actual.toString().equalsIgnoreCase(expected)) {
            throw new IOException("The downloaded update failed its SHA-256 integrity check.");
        }
    }

    private static void extractUpdate(Path zipFile, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            long totalBytes = 0;
            byte[] buffer = new byte[16384];
            while ((entry = zip.getNextEntry()) != null) {
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination) || entry.getName().startsWith("/") || entry.getName().contains("\\")) {
                    throw new IOException("The update package contains an invalid file path.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                Files.createDirectories(output.getParent());
                try (java.io.OutputStream stream = Files.newOutputStream(output)) {
                    int bytesRead;
                    while ((bytesRead = zip.read(buffer)) != -1) {
                        totalBytes += bytesRead;
                        if (totalBytes > MAX_UPDATE_SIZE) {
                            throw new IOException("The extracted update package is larger than 1 GB.");
                        }
                        stream.write(buffer, 0, bytesRead);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static void validateApplicationDirectory(Path directory) throws IOException {
        if (!Files.isRegularFile(directory.resolve("EduPortal.jar"))
                || !Files.isDirectory(directory.resolve("lib"))
                || !Files.isRegularFile(directory.resolve("runtime").resolve("bin").resolve("javaw.exe"))) {
            throw new IOException("Run EduPortal from its prepared distribution folder to install updates.");
        }
    }

    private static void validateUpdatePackage(Path directory) throws IOException {
        if (!Files.isRegularFile(directory.resolve("EduPortal.jar"))
                || !Files.isDirectory(directory.resolve("lib"))
                || !Files.isRegularFile(directory.resolve("runtime").resolve("bin").resolve("javaw.exe"))) {
            throw new IOException("The release ZIP must contain EduPortal.jar, lib, and runtime at its root.");
        }
    }

    private static void launchInstallerHelper(Path appDirectory, Path stageDirectory, Path updateZip)
            throws IOException {
        Path helper = Files.createTempFile("eduportal-update-", ".ps1");
        String controlPath = System.getenv("EDUPORTAL_LAUNCHER_CONTROL");
        String updateMarker = controlPath == null || controlPath.isBlank() ? "" : controlPath + ".updating";
        String script = "$ErrorActionPreference = 'Stop'\n"
                + "$processId = " + ProcessHandle.current().pid() + "\n"
                + "$app = " + psQuote(appDirectory.toString()) + "\n"
                + "$stage = " + psQuote(stageDirectory.toString()) + "\n"
                + "$archive = " + psQuote(updateZip.toString()) + "\n"
                + "$control = " + psQuote(controlPath == null ? "" : controlPath) + "\n"
                + "$updateMarker = " + psQuote(updateMarker) + "\n"
                + "$newProcess = $null\n"
                + "try {\n"
                + "  while (Get-Process -Id $processId -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 1 }\n"
                + "  Get-ChildItem -LiteralPath $stage -Force | ForEach-Object {\n"
                + "    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $app $_.Name) -Recurse -Force\n"
                + "  }\n"
                + "  $java = Join-Path $app 'runtime\\bin\\javaw.exe'\n"
                + "  $newProcess = Start-Process -FilePath $java -WorkingDirectory $app -PassThru -ArgumentList @('--module-path=lib',"
                + "'--add-modules=javafx.controls,javafx.fxml,javafx.graphics',"
                + "'--enable-native-access=ALL-UNNAMED','-cp','EduPortal.jar;lib/*','com.eduportal.Main')\n"
                + "  if ($control) { Set-Content -LiteralPath $control -Value $newProcess.Id }\n"
                + "} catch {\n"
                + "  if ($newProcess) { Stop-Process -Id $newProcess.Id -Force -ErrorAction SilentlyContinue }\n"
                + "  Add-Content -LiteralPath (Join-Path $env:LOCALAPPDATA 'EduPortal\\update-error.log')"
                + " -Value ((Get-Date).ToString('s') + ' ' + $_.Exception.Message)\n"
                + "} finally {\n"
                + "  if ($updateMarker) { Remove-Item -LiteralPath $updateMarker -Force -ErrorAction SilentlyContinue }\n"
                + "  Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue\n"
                + "  Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue\n"
                + "  Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue\n"
                + "}\n";
        try {
            if (!updateMarker.isEmpty()) {
                Files.writeString(Path.of(updateMarker), "pending", StandardCharsets.UTF_8);
            }
            Files.writeString(helper, script, StandardCharsets.UTF_8);
            new ProcessBuilder("powershell.exe", "-WindowStyle", "Hidden", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", helper.toString()).start();
        } catch (IOException e) {
            if (!updateMarker.isEmpty()) Files.deleteIfExists(Path.of(updateMarker));
            Files.deleteIfExists(helper);
            throw new IOException("Could not start the update installer helper.", e);
        }
    }

    private static String psQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static ReleaseAsset findUpdateAsset(String json) {
        Matcher assets = Pattern.compile("\"assets\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        if (!assets.find()) return null;
        Matcher objects = Pattern.compile("\\{([^{}]*)}", Pattern.DOTALL).matcher(assets.group(1));
        ReleaseAsset fallback = null;
        while (objects.find()) {
            String asset = objects.group(1);
            String name = jsonStringField(asset, "name");
            String downloadUrl = jsonStringField(asset, "browser_download_url");
            if (name == null || downloadUrl == null || !name.toLowerCase(Locale.ROOT).endsWith(".zip")) continue;
            ReleaseAsset candidate = new ReleaseAsset(downloadUrl, jsonStringField(asset, "digest"));
            if ("eduportal-update.zip".equalsIgnoreCase(name)) return candidate;
            if (name.toLowerCase(Locale.ROOT).contains("eduportal")) fallback = candidate;
        }
        return fallback;
    }

    private static String jsonStringField(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\/", "/")
                .replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static Integer versionCompare(String first, String second) {
        long[] a = versionNumbers(first);
        long[] b = versionNumbers(second);
        if (a == null || b == null) return null;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            long left = i < a.length ? a[i] : 0;
            long right = i < b.length ? b[i] : 0;
            if (left != right) return Long.compare(left, right);
        }
        return 0;
    }

    private static long[] versionNumbers(String version) {
        Matcher matcher = Pattern.compile("^[vV]?(\\d+(?:\\.\\d+){1,3})(?:[-+].*)?$").matcher(version.trim());
        if (!matcher.matches()) return null;
        String[] parts = matcher.group(1).split("\\.");
        long[] numbers = new long[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) numbers[i] = Long.parseLong(parts[i]);
            return numbers;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UpdateStatus status(State state, String message, String version, String url, String sha256) {
        return new UpdateStatus(state, message, version, url, sha256);
    }

    private static void publish(Consumer<UpdateStatus> listener, UpdateStatus status) {
        Platform.runLater(() -> listener.accept(status));
    }

    private static void deleteFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("[AutoUpdater] Could not remove temporary file: " + path);
        }
    }

    private static void deleteTree(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (java.util.stream.Stream<Path> paths = Files.walk(path)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(AutoUpdater::deleteFile);
        } catch (IOException e) {
            System.err.println("[AutoUpdater] Could not clean temporary directory: " + path);
        }
    }

    private static final class ReleaseAsset {
        private final String downloadUrl;
        private final String sha256;

        private ReleaseAsset(String downloadUrl, String sha256) {
            this.downloadUrl = downloadUrl;
            this.sha256 = sha256;
        }
    }
}
