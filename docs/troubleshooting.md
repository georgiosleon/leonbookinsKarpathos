# Troubleshooting

## Login / access
- **Invalid username or password**: verify credentials. Default dev seed accounts are `admin/user/guest` with password `1qaz2wsx3edc` only when those accounts were seeded by the app; production should use `APP_*` environment variables.
- **Signed in but app shows license activation**: a fresh install requires ADMIN license activation. Use the Installation ID shown on screen to obtain a signed key. For dev/test only, set `APP_LICENSE_BYPASS=true`.
- **403 / CSRF errors**: refresh the page to get a fresh CSRF token; ensure browser allows cookies for the app origin.

## Licensing
- **Invalid license key**: confirm the key matches the exact Installation ID and was signed for the correct public key (`APP_LICENSE_PUBLIC_KEY` in production). Compose forwards only the variables listed in `compose.yaml`; add this key as an explicit `environment` entry there (or run the JAR directly). Otherwise `LicenseVerifier` falls back to the embedded development public key.
- **Activation should not be bypassed in production**: run with `prod` profile and `APP_LICENSE_BYPASS=false`.

## Database / startup
- **Migration failure on startup**: back up `bookings.db` before upgrades. Only V1-baselined/current schemas are supported; pre-V1 DBs must be exported/reimported or upgraded outside the supported path.
- **SQLite locked / busy**: ensure only one app process uses the same DB file. Do not share a live SQLite file over network shares.
- **Port 8080 already in use**: stop the conflicting process or set `server.port` / `SERVER_PORT`.

## Backup / restore
- Always stop the app before restore. Restore creates/overwrites the target DB path as documented by CLI.
- Keep backup encryption passphrase separate from DB/seed secrets.
- Validate restore by starting the app and checking key screens/reports.

## Support
- Download **Support bundle** from the app (authenticated users) or `/api/v1/support-bundle`. It includes structured app info (no secrets, install ID, or customer PII) and a best-effort sanitized log tail that may still contain install IDs or customer names.
- Include app version, OS, steps to reproduce, and relevant logs when reporting issues.
