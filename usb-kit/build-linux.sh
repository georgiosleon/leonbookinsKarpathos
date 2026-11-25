#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
source "$SCRIPT_DIR/scripts/common.sh"

usage() {
  echo "Usage: $0 --usb-root DIR --license-db FILE"
  echo "  --usb-root   Mounted USB directory, e.g. /media/USB"
  echo "  --license-db Activated bookings.db to copy onto the USB"
  echo
  echo "Build the Linux package first with: ./install/package.sh linux"
}

USB_ROOT=""
LICENSE_DB=""
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

REPO="$(usb_kit_repo_root)"
VERSION="$(usb_kit_read_version "$REPO")"
[[ -n "$VERSION" ]] || usb_kit_fail "Could not read the project version from pom.xml"
ARCH="$(uname -m)"
ARCHIVE_NAME="LeonBookings-${VERSION}-linux-${ARCH}.tar.gz"
ARCHIVE="$REPO/install/dist/linux/$ARCHIVE_NAME"
[[ -f "$ARCHIVE" ]] || usb_kit_fail "Missing $ARCHIVE. Run ./install/package.sh linux first."

KIT="$(usb_kit_prepare_kit_dir "$USB_ROOT" "$REPO")"
DEST="$KIT/packages/linux-$ARCH"
mkdir -p "$DEST"
cp "$ARCHIVE" "$DEST/"
usb_kit_copy_license_database "$LICENSE_DB" "$KIT"
usb_kit_write_checksum "$DEST/$ARCHIVE_NAME" "$KIT"

echo "Linux $ARCH package added to $KIT"
