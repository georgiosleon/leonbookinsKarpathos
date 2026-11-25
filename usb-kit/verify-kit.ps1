param(
    [Parameter(Mandatory = $true)][string]$UsbRoot
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Kit = Join-Path $UsbRoot "LEON-BOOKINGS"
if (-not (Test-Path -LiteralPath $UsbRoot -PathType Container)) {
    throw "USB root is not a directory: $UsbRoot"
}

[xml]$Pom = Get-Content -LiteralPath (Join-Path $Root "pom.xml")
$Version = $Pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='version']").InnerText
if (-not $Version) {
    throw "Could not read the project version from pom.xml"
}

$errors = 0
foreach ($required in @("README.txt", "USB-KIT.md", "start-linux.sh", "start-macos.sh", "start-windows.ps1", "licensed-database/bookings.db")) {
    if (Test-Path -LiteralPath (Join-Path $Kit $required) -PathType Leaf) {
        Write-Host "OK   $required"
    } else {
        Write-Host "MISS $required"
        $errors++
    }
}

$archives = @(
    "packages/linux-x86_64/LeonBookings-$Version-linux-x86_64.tar.gz",
    "packages/windows-x64/LeonBookings-$Version-windows-x64.zip",
    "packages/macos-x86_64/LeonBookings-$Version-macos-x86_64.zip",
    "packages/macos-arm64/LeonBookings-$Version-macos-arm64.zip"
)

foreach ($relative in $archives) {
    $file = Join-Path $Kit $relative
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        Write-Host "MISS $relative"
        $errors++
        continue
    }

    $checksumFile = Join-Path $Kit "checksums/$(Split-Path -Leaf $file).sha256"
    if (-not (Test-Path -LiteralPath $checksumFile -PathType Leaf)) {
        Write-Host "WARN $relative has no checksum file"
        continue
    }

    $expected = ((Get-Content -LiteralPath $checksumFile -TotalCount 1).Split(' ')[0]).ToLowerInvariant()
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
    if ($expected -eq $actual) {
        Write-Host "OK   $relative"
    } else {
        Write-Host "BAD  $relative checksum mismatch"
        $errors++
    }
}

if ($errors -gt 0) {
    throw "USB kit verification failed with $errors problem(s)."
}
Write-Host "USB kit verification passed."
