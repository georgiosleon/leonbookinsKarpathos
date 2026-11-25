# USB test kit

This folder builds a **data USB for demos and release smoke tests**. It is not an
ISO and not a bootable image. Leon Bookings is not one universal executable: each
supported OS needs its own portable package with its own bundled Java 25 runtime.

The scripts here only **assemble already-built portable archives**. They do not
cross-build packages. Build each package on its matching OS, then copy it onto
the USB.

## What is on the USB

```text
USB/
  LEON-BOOKINGS/
    README.txt
    USB-KIT.md
    start-linux.sh
    start-macos.sh
    start-windows.ps1
    licensed-database/
      bookings.db
    packages/
      linux-x86_64/     LeonBookings-<version>-linux-x86_64.tar.gz
      windows-x64/      LeonBookings-<version>-windows-x64.zip
      macos-x86_64/     LeonBookings-<version>-macos-x86_64.zip
      macos-arm64/      LeonBookings-<version>-macos-arm64.zip
    run/
      <created on first launch>
    checksums/
      <archive-name>.sha256
```

Use a **writable exFAT USB** if you want one disk that works on Windows, Linux,
Ubuntu, Fedora, and macOS. Treat the USB as sensitive: `bookings.db` contains
customer data once demo data is replaced with real data.

## License rule

A license is **not a file the app reads from the USB**. Activation lives inside
`bookings.db`:

1. Start the app from a throwaway copy.
2. Read the Installation ID shown in the activation screen.
3. Sign it externally with the production private key:
   ```sh
   java --source 25 tools/LicenseSigner.java \
     /secure/path/production-license-private.pkcs8.b64 \
     <installId> "Customer Name" "+30..." "customer@example.com"
   ```
4. Paste the signed key into the activation screen.
5. Copy that activated `bookings.db` to `licensed-database/bookings.db` on the USB.

Because the install ID stays inside that database, the same license works from
every OS package as long as you use that database copy. Do **not** put the
production private key on the USB.

## Build the Linux package

On Linux (Ubuntu or Fedora x86-64):

```sh
./install/package.sh linux
./usb-kit/build-linux.sh \
  --usb-root /media/USB \
  --license-db /secure/path/activated-bookings.db
```

One Linux x86-64 archive is intended for both Ubuntu and Fedora. Smoke-test it
on both because the bundled Java runtime still depends on the host glibc.

## Build the Windows package

In PowerShell on Windows 11 x86-64:

```powershell
.\install\package.ps1 portable
.\usb-kit\build-windows.ps1 `
  -UsbRoot E:\ `
  -LicenseDatabase C:\secure\activated-bookings.db
```

The build requires JDK 25 with `jpackage` and WiX Toolset 3 only for MSI builds;
the portable ZIP does not need WiX.

## Build the macOS packages

On an Intel Mac:

```sh
./install/package.sh portable
./usb-kit/build-macos.sh \
  --usb-root /Volumes/USB \
  --license-db /secure/path/activated-bookings.db \
  --arch x86_64
```

On an Apple Silicon Mac:

```sh
./install/package.sh portable
./usb-kit/build-macos.sh \
  --usb-root /Volumes/USB \
  --license-db /secure/path/activated-bookings.db \
  --arch arm64
```

Run the macOS build once on each architecture and copy both archives to the
same USB. The script refuses to copy an architecture that does not match the
build host.

## Verify the kit

From Linux/macOS:

```sh
./usb-kit/verify-kit.sh --usb-root /media/USB
```

From Windows PowerShell:

```powershell
.\usb-kit\verify-kit.ps1 -UsbRoot E:\
```

This checks that all four archives, both README files, the three startup scripts,
and the licensed database exist, and verifies the generated SHA-256 checksums.

## Run from each platform

The startup scripts do the extraction, copy the licensed database, and launch the
correct executable. Run one platform at a time and stop the app before switching
machines.

### Windows 11 x86-64

From PowerShell in `LEON-BOOKINGS`:

```powershell
.\start-windows.ps1
```

Or extract manually:

```powershell
cd .\LeonBookings
.\LeonBookings.exe
```

Unsigned portable builds may show a SmartScreen warning.

### Linux x86-64 / Ubuntu / Fedora

From a terminal in `LEON-BOOKINGS`:

```sh
./start-linux.sh
```

Or extract manually:

```sh
tar -xzf LeonBookings-<version>-linux-x86_64.tar.gz
cd LeonBookings
cp /media/USB/LEON-BOOKINGS/licensed-database/bookings.db .
./bin/LeonBookings
```

FAT32/exFAT does not preserve Unix execute bits. If you must run directly from
the USB, use `chmod +x bin/LeonBookings` first. Extracting to the local disk is
more reliable.

### macOS 13+ Intel / Apple Silicon

From Terminal in `LEON-BOOKINGS`, on Intel:

```sh
./start-macos.sh x86_64
```

On Apple Silicon:

```sh
./start-macos.sh arm64
```

Or extract manually:

```sh
cd LeonBookings
cp /Volumes/USB/LEON-BOOKINGS/licensed-database/bookings.db .
./LeonBookings.app/Contents/MacOS/LeonBookings
```

Do not put `bookings.db` inside `LeonBookings.app`; the launcher reads it from
the current working directory. For unsigned builds, macOS may require:

```sh
xattr -d com.apple.quarantine LeonBookings.app
```

Public distribution needs signing/notarization.

## Operating rules

- Run **one OS at a time** against the licensed database.
- Stop the application before switching machines or removing the USB.
- Do not run two OS instances against the same `bookings.db` simultaneously.
- The app writes to the database; the USB must be writable.
- Keep the private key outside the repository and outside the USB kit.
- This USB is for testing/demos. It is not a bootable product or an installer.
