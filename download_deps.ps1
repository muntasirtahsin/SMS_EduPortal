$urls = @(
    'https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-win.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-win.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-win.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2.jar',
    'https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-win.jar',
    'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar',
    'https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar',
    'https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar',
    'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar'
)

foreach ($u in $urls) {
    $fileName = Split-Path $u -Leaf
    $dest = Join-Path "lib" $fileName
    if (-not (Test-Path $dest)) {
        Write-Host "Downloading $fileName..."
        Invoke-WebRequest -Uri $u -OutFile $dest
    } else {
        Write-Host "$fileName already present."
    }
}

$sqliteVersioned = Join-Path "lib" "sqlite-jdbc-3.45.1.0.jar"
$sqliteRuntime = Join-Path "lib" "sqlite-jdbc.jar"
if (-not (Test-Path $sqliteRuntime) -and (Test-Path $sqliteVersioned)) {
    Copy-Item $sqliteVersioned $sqliteRuntime
}
