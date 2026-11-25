#!/usr/bin/env bash
set -euo pipefail

usb_kit_fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usb_kit_repo_root() {
  cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd
}

usb_kit_read_version() {
  local repo="$1"
  sed -n '/<\/parent>/,$s/.*<version>\([^<]*\)<\/version>.*/\1/p' \
    "$repo/pom.xml" | head -n 1
}

usb_kit_prepare_kit_dir() {
  local usb_root="$1"
  local repo="$2"
  [[ -d "$usb_root" ]] || usb_kit_fail "USB root is not a directory: $usb_root"
  [[ -w "$usb_root" ]] || usb_kit_fail "USB root is not writable: $usb_root"

  local kit="$usb_root/LEON-BOOKINGS"
  mkdir -p "$kit/checksums" "$kit/licensed-database" "$kit/run"
  for platform in linux-x86_64 windows-x64 macos-x86_64 macos-arm64; do
    mkdir -p "$kit/packages/$platform"
  done
  cp "$repo/README.md" "$kit/README.txt"
  cp "$repo/usb-kit/README.md" "$kit/USB-KIT.md"
  cp "$repo/usb-kit/start/start-linux.sh" "$kit/start-linux.sh"
  cp "$repo/usb-kit/start/start-macos.sh" "$kit/start-macos.sh"
  cp "$repo/usb-kit/start/start-windows.ps1" "$kit/start-windows.ps1"
  chmod +x "$kit/start-linux.sh" "$kit/start-macos.sh"
  printf '%s\n' "$kit"
}

usb_kit_copy_license_database() {
  local source="$1"
  local kit="$2"
  [[ -f "$source" ]] || usb_kit_fail "Licensed database not found: $source"
  cp "$source" "$kit/licensed-database/bookings.db"
}

usb_kit_hash() {
  local file="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  else
    usb_kit_fail "Need sha256sum (Linux) or shasum -a 256 (macOS) to verify the kit"
  fi
}

usb_kit_write_checksum() {
  local file="$1"
  local kit="$2"
  local name hash
  name="$(basename "$file")"
  hash="$(usb_kit_hash "$file")"
  printf '%s  %s\n' "$hash" "$name" > "$kit/checksums/$name.sha256"
}
