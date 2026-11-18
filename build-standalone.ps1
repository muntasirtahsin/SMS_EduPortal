param(
    [ValidatePattern('^[vV]?\d+(\.\d+){1,3}$')]
    [string]$Version = "v1.0.0"
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$packagingScript = Join-Path $projectRoot "prepare-launch4j.ps1"
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $packagingScript -Version $Version
if ($LASTEXITCODE -ne 0) {
    throw "Application package preparation failed with exit code $LASTEXITCODE."
}

$compiler = Get-Command csc.exe -ErrorAction SilentlyContinue
if ($compiler) {
    $compilerPath = $compiler.Source
} else {
    $compilerPath = Join-Path $env:WINDIR "Microsoft.NET\Framework64\v4.0.30319\csc.exe"
    if (-not (Test-Path $compilerPath)) {
        $compilerPath = Join-Path $env:WINDIR "Microsoft.NET\Framework\v4.0.30319\csc.exe"
    }
}
if (-not (Test-Path $compilerPath)) {
    throw "The .NET Framework C# compiler was not found."
}

$payload = Join-Path $projectRoot "dist\EduPortal\EduPortal-update.zip"
$output = Join-Path $projectRoot "dist\EduPortal\EduPortal.exe"
$icon = Join-Path $projectRoot "eduportal.ico"
$launcher = Join-Path $projectRoot "Launcher.cs"
foreach ($requiredFile in @($payload, $icon, $launcher)) {
    if (-not (Test-Path $requiredFile)) {
        throw "Required standalone launcher input is missing: $requiredFile"
    }
}

& $compilerPath /nologo /target:winexe "/out:$output" "/win32icon:$icon" `
    "/resource:$payload,EduPortalPayload.zip" `
    /reference:System.IO.Compression.dll `
    /reference:System.IO.Compression.FileSystem.dll `
    /reference:System.Windows.Forms.dll `
    $launcher
if ($LASTEXITCODE -ne 0) {
    throw "Standalone EXE compilation failed with exit code $LASTEXITCODE."
}

Write-Host "Single-file EduPortal EXE created at: $output"
