$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Type = if ($args.Count -gt 0) { $args[0] } else { "msi" }
if ($Type -ne "msi") {
    throw "Windows supports the msi package type."
}

function Get-CommandVersion([string]$Name, [string[]]$Arguments) {
    $Command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $Command) {
        throw "$Name is missing from PATH. Install JDK 25 (including jpackage); Maven is provided by the project wrapper. See install/README.md."
    }
    $Output = & $Command.Source @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not run $Name -- verify its installation and PATH."
    }
    return ($Output | Select-Object -First 1).ToString()
}

$JavaVersionOutput = Get-CommandVersion "java" @("-version")
$JavacVersionOutput = Get-CommandVersion "javac" @("-version")
$JpackageVersionOutput = Get-CommandVersion "jpackage" @("--version")

if ($JavaVersionOutput -notmatch 'version "25(?:[.+-]|")') {
    throw "java must be JDK 25 (found '$JavaVersionOutput')."
}
if ($JavacVersionOutput -notmatch '^javac 25(?:[.+-]|$)') {
    throw "javac must be JDK 25 (found '$JavacVersionOutput')."
}
if ($JpackageVersionOutput -notmatch '^25(?:[.+-]|$)') {
    throw "jpackage must be from JDK 25 (found '$JpackageVersionOutput')."
}
$JavaProperties = & java -XshowSettings:properties -version 2>&1 |
    ForEach-Object { $_.ToString() }
$JavaHomeLine = $JavaProperties |
    Where-Object { $_ -match '^\s*java\.home\s*=' } |
    Select-Object -First 1
if (-not $JavaHomeLine -or $JavaHomeLine -notmatch '^\s*java\.home\s*=\s*(.+?)\s*$') {
    throw "Could not determine the JDK home used by java."
}
$JavaHome = $Matches[1].Trim()
$env:JAVA_HOME = $JavaHome
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome "jmods") -PathType Container)) {
    throw "JDK module files are missing from '$JavaHome\jmods'. Install a full JDK 25 that includes jmods; jpackage needs these files to create the bundled runtime."
}
if (-not (Test-Path -LiteralPath (Join-Path $Root "mvnw.cmd") -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $Root ".mvn/wrapper/maven-wrapper.properties") -PathType Leaf)) {
    throw "Maven Wrapper files are missing. Restore mvnw.cmd and .mvn/wrapper from the project."
}

$WixCandle = Get-Command "candle.exe" -ErrorAction SilentlyContinue
$WixLight = Get-Command "light.exe" -ErrorAction SilentlyContinue
if (-not $WixCandle -or -not $WixLight) {
    throw "WiX Toolset 3 must be installed with candle.exe and light.exe on PATH to build MSI packages. See install/README.md."
}

[xml]$Pom = Get-Content (Join-Path $Root "pom.xml")
$Version = $Pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='version']").InnerText
if (-not $Version) {
    throw "Could not read the project version from pom.xml."
}

Push-Location $Root
try {
    & (Join-Path $Root "mvnw.cmd") -B package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed with exit code $LASTEXITCODE."
    }

    $LauncherClasses = Join-Path $Root "target/desktop-launcher/classes"
    $PackageInput = Join-Path $Root "target/desktop-package"
    $Output = Join-Path $Root ("target/desktop-output-" + [guid]::NewGuid().ToString())
    $Dist = Join-Path $Root "install/dist/msi"
    New-Item -ItemType Directory -Force $LauncherClasses, $PackageInput, $Output, $Dist | Out-Null

    & javac --release 25 -d $LauncherClasses `
        (Join-Path $Root "install/desktop-launcher/src/com/leonsoft/desktop/DesktopLauncher.java")
    if ($LASTEXITCODE -ne 0) {
        throw "Could not compile the desktop launcher."
    }
    & jar --create --file (Join-Path $PackageInput "desktop-launcher.jar") `
        --main-class com.leonsoft.desktop.DesktopLauncher -C $LauncherClasses .
    if ($LASTEXITCODE -ne 0) {
        throw "Could not package the desktop launcher."
    }
    Copy-Item (Join-Path $Root "target/leonbookinsKarpathos-$Version.jar") `
        (Join-Path $PackageInput "leonbookinsKarpathos.jar") -Force

    & jpackage `
        --type msi `
        --name LeonBookings `
        --app-version $Version `
        --input $PackageInput `
        --main-jar desktop-launcher.jar `
        --main-class com.leonsoft.desktop.DesktopLauncher `
        --add-modules ALL-MODULE-PATH `
        --dest $Output `
        --win-upgrade-uuid 967737ef-d283-47ab-a360-0d71b4b33060 `
        --win-menu `
        --win-shortcut `
        --win-dir-chooser `
        --win-per-user-install
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }
    Copy-Item (Join-Path $Output "*.msi") $Dist -Force
    Write-Host "Installer created in install/dist/msi/"
} finally {
    Pop-Location
}
