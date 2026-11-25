#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

ARCH="${1:-$(uname -m)}"
HOST_ARCH="$(uname -m)"
case "$ARCH" in
  x86_64|arm64) ;;
  *) fail "Unsupported macOS architecture: $ARCH" ;;
esac
[[ "$ARCH" == "$HOST_ARCH" ]] || fail "Run the $ARCH package on a $ARCH Mac; this host reports $HOST_ARCH."

KIT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB="$KIT/licensed-database/bookings.db"
RUN="$KIT/run/macos-$ARCH"
APP="$RUN/LeonBookings.app"

[[ -f "$DB" ]] || fail "Missing licensed database: $DB"

shopt -s nullglob
archives=("$KIT"/packages/macos-"$ARCH"/LeonBookings-*-macos-"$ARCH".zip)
(( ${#archives[@]} == 1 )) || fail "Expected exactly one macOS $ARCH archive in $KIT/packages/macos-$ARCH"

command -v ditto >/dev/null 2>&1 || fail "ditto is required to extract the macOS package."

mkdir -p "$RUN"
if [[ ! -x "$APP/Contents/MacOS/LeonBookings" ]]; then
  ditto -x -k "${archives[0]}" "$RUN"
fi
chmod +x "$APP/Contents/MacOS/LeonBookings" "$APP/Contents/runtime/Contents/Home/bin/java" 2>/dev/null || true
[[ -x "$APP/Contents/MacOS/LeonBookings" ]] || fail "Portable macOS launcher is missing: $APP/Contents/MacOS/LeonBookings"

if [[ ! -f "$RUN/bookings.db" ]]; then
  cp "$DB" "$RUN/bookings.db"
fi

cd "$RUN"
exec "./LeonBookings.app/Contents/MacOS/LeonBookings"
