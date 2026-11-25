param(
    [Parameter(Mandatory = $true)][string]$UsbRoot,
    [Parameter(Mandatory = $true)][string]$LicenseDatabase
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath $UsbRoot -PathType Container)) {
    throw "USB root is not a directory: $UsbRoot"
}
if (-not (Test-Path -LiteralPath $LicenseDatabase -PathType Leaf)) {
    throw "Licensed database not found: $LicenseDatabase"
}

[xml]$Pom = Get-Content -LiteralPath (Join-Path $Root "pom.xml")
$Version = $Pom.SelectSingleNode("/*[local-name()='project']/*[local-name()='version']").InnerText
if (-not $Version) {
    throw "Could not read the project version from pom.xml"
}

$DetectedArchitecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
if ($DetectedArchitecture -in @("x64", "x86_64", "amd64")) {
    $Architecture = "x64"
} else {
    throw "Only Windows x64 (x86-64) portable packages are supported; detected '$DetectedArchitecture'."
}

$ArchiveName = "LeonBookings-$Version-windows-$Architecture.zip"
$Archive = Join-Path $Root "install/dist/portable/$ArchiveName"
if (-not (Test-Path -LiteralPath $Archive -PathType Leaf)) {
    throw "Missing $Archive. Run .\install\package.ps1 portable first."
}

$Kit = Join-Path $UsbRoot "LEON-BOOKINGS"
$Destination = Join-Path $Kit "packages/windows-$Architecture"
New-Item -ItemType Directory -Force -Path @($Destination, (Join-Path $Kit "checksums"), (Join-Path $Kit "licensed-database")) | Out-Null
Copy-Item -LiteralPath (Join-Path $Root "README.md") -Destination (Join-Path $Kit "README.txt") -Force
Copy-Item -LiteralPath (Join-Path $Root "usb-kit/README.md") -Destination (Join-Path $Kit "USB-KIT.md") -Force
Copy-Item -LiteralPath $LicenseDatabase -Destination (Join-Path $Kit "licensed-database/bookings.db") -Force
Copy-Item -LiteralPath $Archive -Destination (Join-Path $Destination $ArchiveName) -Force

$Hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $Destination $ArchiveName)).Hash.ToLowerInvariant()
Set-Content -LiteralPath (Join-Path $Kit "checksums/$ArchiveName.sha256") `
    -Value "$Hash  $ArchiveName" -Encoding ascii

Write-Host "Windows $Architecture package added to $Kit"
