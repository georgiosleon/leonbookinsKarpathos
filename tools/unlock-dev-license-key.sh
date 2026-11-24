#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENC="$ROOT/tools/dev-license-private.pkcs8.b64.enc"
OUT="$ROOT/tools/dev-license-private.pkcs8.b64"
if [[ -f "$OUT" ]]; then
  echo "Dev license private key already exists: $OUT"
  exit 0
fi
if [[ ! -f "$ENC" ]]; then
  echo "Missing encrypted dev key: $ENC" >&2
  exit 1
fi
: "${DEV_LICENSE_KEY_PASSPHRASE:=leonbookins-dev-key}"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -salt -in "$ENC" -out "$OUT" -pass env:DEV_LICENSE_KEY_PASSPHRASE
chmod 600 "$OUT"
echo "Unlocked dev license private key: $OUT"
