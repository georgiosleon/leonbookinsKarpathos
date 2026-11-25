#!/usr/bin/env bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
source "$SCRIPT_DIR/scripts/common.sh"
set +e

usage() {
  echo "Usage: $0 --usb-root DIR"
  echo "  --usb-root   Mounted USB directory containing LEON-BOOKINGS/"
}

USB_ROOT=""
while (($#)); do
  case "$1" in
    --usb-root)
      (($# >= 2)) || { echo "ERROR: --usb-root requires a value" >&2; exit 2; }
      USB_ROOT="$2"
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
[[ -d "$USB_ROOT" ]] || { echo "ERROR: USB root is not a directory: $USB_ROOT" >&2; exit 1; }

REPO="$(usb_kit_repo_root)"
VERSION="$(usb_kit_read_version "$REPO")"
KIT="$USB_ROOT/LEON-BOOKINGS"

if ! command -v sha256sum >/dev/null 2>&1 && ! command -v shasum >/dev/null 2>&1; then
  echo "ERROR: Need sha256sum (Linux) or shasum -a 256 (macOS) to verify the kit" >&2
  exit 1
fi

errors=0
for required in "README.txt" "USB-KIT.md" "start-linux.sh" "start-macos.sh" "start-windows.ps1" "licensed-database/bookings.db"; do
  if [[ -f "$KIT/$required" ]]; then
    echo "OK   $required"
  else
    echo "MISS $required"
    errors=$((errors + 1))
  fi
done

relative_archives=(
  "packages/linux-x86_64/LeonBookings-${VERSION}-linux-x86_64.tar.gz"
  "packages/windows-x64/LeonBookings-${VERSION}-windows-x64.zip"
  "packages/macos-x86_64/LeonBookings-${VERSION}-macos-x86_64.zip"
  "packages/macos-arm64/LeonBookings-${VERSION}-macos-arm64.zip"
)

for relative in "${relative_archives[@]}"; do
  file="$KIT/$relative"
  if [[ ! -f "$file" ]]; then
    echo "MISS $relative"
    errors=$((errors + 1))
    continue
  fi

  checksum_file="$KIT/checksums/$(basename "$file").sha256"
  if [[ ! -f "$checksum_file" ]]; then
    echo "WARN $relative has no checksum file"
    continue
  fi

  expected="$(awk '{print $1}' "$checksum_file")"
  actual="$(usb_kit_hash "$file")"
  if [[ "$expected" == "$actual" ]]; then
    echo "OK   $relative"
  else
    echo "BAD  $relative checksum mismatch"
    errors=$((errors + 1))
  fi
done

if ((errors > 0)); then
  echo "USB kit verification failed with $errors problem(s)." >&2
  exit 1
fi
echo "USB kit verification passed."
