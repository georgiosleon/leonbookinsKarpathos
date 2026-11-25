#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

KIT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB="$KIT/licensed-database/bookings.db"
RUN="$KIT/run/linux-x86_64"
APP="$RUN/LeonBookings"

[[ -f "$DB" ]] || fail "Missing licensed database: $DB"

shopt -s nullglob
archives=("$KIT"/packages/linux-x86_64/LeonBookings-*-linux-x86_64.tar.gz)
(( ${#archives[@]} == 1 )) || fail "Expected exactly one Linux x86-64 archive in $KIT/packages/linux-x86_64"

mkdir -p "$RUN"
if [[ ! -x "$APP/bin/LeonBookings" ]]; then
  tar -xzf "${archives[0]}" -C "$RUN"
fi
[[ -x "$APP/bin/LeonBookings" ]] || fail "Portable Linux launcher is missing: $APP/bin/LeonBookings"

if [[ ! -f "$APP/bookings.db" ]]; then
  cp "$DB" "$APP/bookings.db"
fi

cd "$APP"
exec ./bin/LeonBookings
