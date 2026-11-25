#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
source "$SCRIPT_DIR/scripts/common.sh"

usage() {
  echo "Usage: $0 --usb-root DIR --license-db FILE [--arch x86_64|arm64]"
  echo "  --usb-root   Mounted USB directory, e.g. /Volumes/USB"
  echo "  --license-db Activated bookings.db to copy onto the USB"
  echo "  --arch       Architecture built on this Mac; defaults to uname -m"
  echo
  echo "Build the package on each Mac with: ./install/package.sh portable"
}

USB_ROOT=""
LICENSE_DB=""
ARCH=""
while (($#)); do
  case "$1" in
    --usb-root)
      (($# >= 2)) || usb_kit_fail "--usb-root requires a value"
      USB_ROOT="$2"
      shift 2
      ;;
    --license-db)
      (($# >= 2)) || usb_kit_fail "--license-db requires a value"
      LICENSE_DB="$2"
      shift 2
      ;;
    --arch)
      (($# >= 2)) || usb_kit_fail "--arch requires a value"
      ARCH="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
done

[[ -n "$USB_ROOT" ]] || { usage >&2; exit 2; }
[[ -n "$LICENSE_DB" ]] || { usage >&2; exit 2; }

HOST_ARCH="$(uname -m)"
[[ -z "$ARCH" ]] && ARCH="$HOST_ARCH"
case "$ARCH" in
  x86_64|arm64) ;;
  *) usb_kit_fail "Unsupported macOS architecture: $ARCH" ;;
esac
[[ "$ARCH" == "$HOST_ARCH" ]] || usb_kit_fail \
  "Build on an $ARCH Mac; this host reports $HOST_ARCH. Portable macOS packages are architecture-specific."

REPO="$(usb_kit_repo_root)"
VERSION="$(usb_kit_read_version "$REPO")"
[[ -n "$VERSION" ]] || usb_kit_fail "Could not read the project version from pom.xml"
ARCHIVE_NAME="LeonBookings-${VERSION}-macos-${ARCH}.zip"
ARCHIVE="$REPO/install/dist/portable/$ARCHIVE_NAME"
[[ -f "$ARCHIVE" ]] || usb_kit_fail "Missing $ARCHIVE. Run ./install/package.sh portable first."

KIT="$(usb_kit_prepare_kit_dir "$USB_ROOT" "$REPO")"
DEST="$KIT/packages/macos-$ARCH"
mkdir -p "$DEST"
cp "$ARCHIVE" "$DEST/"
usb_kit_copy_license_database "$LICENSE_DB" "$KIT"
usb_kit_write_checksum "$DEST/$ARCHIVE_NAME" "$KIT"

echo "macOS $ARCH package added to $KIT"
