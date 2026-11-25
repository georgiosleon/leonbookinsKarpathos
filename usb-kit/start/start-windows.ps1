param(
    [string]$Architecture = "x64"
)

$ErrorActionPreference = "Stop"

if ($Architecture -ne "x64") {
    throw "Only the Windows x64 portable package is supported; got '$Architecture'."
}

$Kit = $PSScriptRoot
$Database = Join-Path $Kit "licensed-database/bookings.db"
$Run = Join-Path $Kit "run/windows-$Architecture"
$App = Join-Path $Run "LeonBookings"

if (-not (Test-Path -LiteralPath $Database -PathType Leaf)) {
    throw "Missing licensed database: $Database"
}

$packages = Join-Path $Kit "packages/windows-$Architecture"
$archives = @(Get-ChildItem -LiteralPath $packages -Filter "LeonBookings-*-windows-$Architecture.zip" -File)
if ($archives.Count -ne 1) {
    throw "Expected exactly one Windows $Architecture archive in $packages; found $($archives.Count)."
}

New-Item -ItemType Directory -Force -Path $Run | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $App "LeonBookings.exe") -PathType Leaf)) {
    Expand-Archive -LiteralPath $archives[0].FullName -DestinationPath $Run -Force
}
if (-not (Test-Path -LiteralPath (Join-Path $App "LeonBookings.exe") -PathType Leaf)) {
    throw "Portable Windows launcher is missing: $(Join-Path $App 'LeonBookings.exe')"
}

$databaseTarget = Join-Path $App "bookings.db"
if (-not (Test-Path -LiteralPath $databaseTarget -PathType Leaf)) {
    Copy-Item -LiteralPath $Database -Destination $databaseTarget
}

Set-Location -LiteralPath $App
& (Join-Path $App "LeonBookings.exe")
