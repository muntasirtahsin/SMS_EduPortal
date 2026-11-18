param(
    [ValidatePattern('^[vV]?\d+(\.\d+){1,3}$')]
    [string]$Version = "v1.0.0"
)

$ErrorActionPreference = "Stop"
$Version = $Version.Trim()
if (-not $Version.StartsWith("v", [System.StringComparison]::OrdinalIgnoreCase)) {
    $Version = "v" + $Version
}

$projectRoot = $PSScriptRoot
$jdkLinker = Get-Command jlink -ErrorAction SilentlyContinue
if (-not $jdkLinker) {
    throw "A full JDK 17+ with jlink is required. Install a JDK and add its bin folder to PATH."
}

$javaHome = Split-Path (Split-Path $jdkLinker.Source -Parent) -Parent
$javac = Join-Path $javaHome "bin\javac.exe"
$jarTool = Join-Path $javaHome "bin\jar.exe"
$jlink = Join-Path $javaHome "bin\jlink.exe"
$jmods = Join-Path $javaHome "jmods"
foreach ($requiredPath in @($javac, $jarTool, $jlink, $jmods)) {
    if (-not (Test-Path $requiredPath)) {
        throw "Required JDK 17+ component not found: $requiredPath"
    }
}

$lib = Join-Path $projectRoot "lib"
$bin = Join-Path $projectRoot "bin"
$dist = Join-Path $projectRoot "dist\EduPortal"
$runtime = Join-Path $dist "runtime"
New-Item -ItemType Directory -Force -Path $bin, $dist, (Join-Path $dist "lib") | Out-Null

$sources = Get-ChildItem (Join-Path $projectRoot "src") -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw "No Java source files found under src."
}

Push-Location $projectRoot
try {
    & $javac --release 17 -encoding UTF-8 -d $bin -cp (Join-Path $lib "*") $sources
    if ($LASTEXITCODE -ne 0) {
        throw "Java compilation failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Set-Content -Path (Join-Path $bin "version.properties") -Value ("version=" + $Version) -Encoding ASCII

foreach ($resource in @("style.css", "eduportal.png")) {
    $resourcePath = Join-Path $projectRoot "src\$resource"
    if (-not (Test-Path $resourcePath)) {
        throw "Required application resource is missing: $resourcePath"
    }
    Copy-Item $resourcePath (Join-Path $bin $resource) -Force
}

$appJar = Join-Path $dist "EduPortal.jar"
if (Test-Path $appJar) {
    Remove-Item $appJar -Force
}
& $jarTool --create --file $appJar --main-class com.eduportal.Main -C $bin .
if ($LASTEXITCODE -ne 0) {
    throw "Application JAR creation failed with exit code $LASTEXITCODE."
}

$requiredLibraries = @(
    "activation-1.1.1.jar",
    "javax.mail-1.6.2.jar",
    "postgresql-42.7.2.jar",
    "sqlite-jdbc.jar",
    "javafx-base-21.0.2-win.jar",
    "javafx-controls-21.0.2-win.jar",
    "javafx-fxml-21.0.2-win.jar",
    "javafx-graphics-21.0.2-win.jar"
)
foreach ($library in $requiredLibraries) {
    $source = Join-Path $lib $library
    if (-not (Test-Path $source)) {
        throw "Required dependency is missing: $source"
    }
    Copy-Item $source (Join-Path $dist "lib\$library") -Force
}

$configFile = Join-Path $projectRoot "config.properties"
if (-not (Test-Path $configFile)) {
    $configFile = Join-Path $projectRoot "config.properties.example"
}
if (-not (Test-Path $configFile)) {
    throw "Required database configuration template is missing."
}
Copy-Item $configFile (Join-Path $dist "config.properties") -Force

foreach ($file in @("eduportal.ico")) {
    $source = Join-Path $projectRoot $file
    if (-not (Test-Path $source)) {
        throw "Required distribution file is missing: $source"
    }
    Copy-Item $source (Join-Path $dist $file) -Force
}

if (-not (Test-Path $runtime)) {
    $modulePath = @(
        $jmods,
        (Join-Path $dist "lib\javafx-base-21.0.2-win.jar"),
        (Join-Path $dist "lib\javafx-graphics-21.0.2-win.jar"),
        (Join-Path $dist "lib\javafx-controls-21.0.2-win.jar"),
        (Join-Path $dist "lib\javafx-fxml-21.0.2-win.jar")
    ) -join ";"
    & $jlink --module-path $modulePath `
        --add-modules javafx.controls,javafx.fxml,java.sql,java.desktop,java.naming,java.logging,java.security.sasl,jdk.crypto.ec `
        --strip-debug --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) {
        throw "Bundled Java runtime creation failed with exit code $LASTEXITCODE."
    }
}

$launch4jConfig = @'
<launch4jConfig>
  <headerType>gui</headerType>
  <outfile>SMS-EduPortal.exe</outfile>
  <jar>EduPortal.jar</jar>
  <icon>eduportal.ico</icon>
  <chdir>.</chdir>
  <classPath>
    <mainClass>com.eduportal.Main</mainClass>
    <cp>lib/*</cp>
  </classPath>
  <jre>
    <path>runtime</path>
    <minVersion>17</minVersion>
    <bundledJre64Bit>true</bundledJre64Bit>
    <opt>--module-path=lib</opt>
    <opt>--add-modules=javafx.controls,javafx.fxml,javafx.graphics</opt>
    <opt>--enable-native-access=ALL-UNNAMED</opt>
  </jre>
  <versionInfo>
    <fileVersion>1.0.0.0</fileVersion>
    <txtFileVersion>1.0.0</txtFileVersion>
    <fileDescription>EduPortal Student Management System</fileDescription>
    <productVersion>1.0.0.0</productVersion>
    <txtProductVersion>1.0.0</txtProductVersion>
    <copyright>Copyright 2026 EduPortal</copyright>
    <productName>EduPortal Student Management System</productName>
    <internalName>EduPortal</internalName>
    <originalFilename>SMS-EduPortal.exe</originalFilename>
    <language>ENGLISH_US</language>
  </versionInfo>
</launch4jConfig>
'@
Set-Content -Path (Join-Path $dist "launch4j.xml") -Value $launch4jConfig -Encoding UTF8

$releaseStage = Join-Path $env:TEMP ("EduPortal-release-" + [Guid]::NewGuid().ToString("N"))
$updateZip = Join-Path $dist "EduPortal-update.zip"
New-Item -ItemType Directory -Force -Path $releaseStage | Out-Null
try {
    Copy-Item $appJar $releaseStage
    Copy-Item (Join-Path $dist "eduportal.ico") $releaseStage
    $releaseLibraries = Join-Path $releaseStage "lib"
    New-Item -ItemType Directory -Force -Path $releaseLibraries | Out-Null
    foreach ($library in $requiredLibraries) {
        Copy-Item (Join-Path $dist "lib\$library") $releaseLibraries
    }
    Copy-Item $runtime $releaseStage -Recurse

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path $updateZip) {
        Remove-Item $updateZip -Force
    }
    $releaseRootPrefix = (Get-Item -LiteralPath $releaseStage).FullName.TrimEnd([char[]]@('\', '/')) +
        [System.IO.Path]::DirectorySeparatorChar
    $archive = [System.IO.Compression.ZipFile]::Open(
        $updateZip,
        [System.IO.Compression.ZipArchiveMode]::Create
    )
    try {
        Get-ChildItem -LiteralPath $releaseStage -Recurse -File | ForEach-Object {
            $fullPath = $_.FullName
            if (-not $fullPath.StartsWith($releaseRootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                throw "Release file is outside the staging directory: $fullPath"
            }
            $entryName = $fullPath.Substring($releaseRootPrefix.Length).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive,
                $_.FullName,
                $entryName,
                [System.IO.Compression.CompressionLevel]::Optimal
            ) | Out-Null
        }
    } finally {
        $archive.Dispose()
    }

    $archive = [System.IO.Compression.ZipFile]::OpenRead($updateZip)
    try {
        $entryNames = @($archive.Entries | ForEach-Object { $_.FullName })
        foreach ($requiredEntry in @("EduPortal.jar", "lib/javafx-controls-21.0.2-win.jar", "runtime/bin/javaw.exe")) {
            if (-not ($entryNames -ccontains $requiredEntry)) {
                throw "Generated update ZIP is missing the required root entry: $requiredEntry"
            }
        }
    } finally {
        $archive.Dispose()
    }
} finally {
    Remove-Item $releaseStage -Recurse -Force
}

Write-Host "Launch4j package prepared at: $dist"
Write-Host "Release ZIP for GitHub: $updateZip"
Write-Host "Run build-standalone.ps1 to create SMS-EduPortal.exe and its Windows ZIP."
