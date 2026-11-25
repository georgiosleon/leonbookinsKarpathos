# i18n review checklist

Generated translations should be reviewed by native speakers before release, especially for new licensing/help/support strings.

## Check
- Run `cd frontend && node scripts/check-translations.js`
- Run `cd frontend && node scripts/check-docs-smoke.js`
- Review keys below in `frontend/src/locales/{el,de,nl,da,fr}/translation.json`

## Recently added/important keys
- License activation, License status, License key, Activate, Installation ID, Copy, Installation ID copied., Could not copy Installation ID.
- This installation requires a valid license key before the application can be used.
- License activation required. Please contact an administrator.
- Provide this Installation ID to generate a license key.
- Activated for {name}
- Customer, Keep one customer record for this installation., Update customer, Create customer, Delete customer
- Activate license, Paste the signed license key for this installation.
- Customer updated., Customer created., Customer deleted., License activated successfully.
- Activate/manage license & customer
- License-enforced/production installs require one-time ADMIN activation; dev/test may use APP_LICENSE_BYPASS=true. The activation screen shows the Installation ID; provide it to the license issuer, then paste the signed license key. Activation takes effect immediately.
- Licensed, Not licensed, Download support bundle
- Install and start, Leon Bookings, Linux portable app, Roles and access
- Sign in with your assigned account. Available actions depend on your role.
- Password *
- Replace the database path as needed. For Docker, run the same db backup command against the mounted /data/bookings.db.
- Replace your-backup.sqlite3.enc with the actual backup filename and adjust the database path as needed.
- Troubleshooting
- License activation required: sign in as ADMIN and activate with the Installation ID, or set APP_LICENSE_BYPASS=true for dev/test only.
- Invalid license key: confirm it matches the Installation ID and correct public key.
- Database migration/startup issue: back up bookings.db and review logs; only V1-baselined/current schemas are supported, and pre-V1 DBs must be exported/reimported or upgraded outside the supported path.
- Backup/restore: stop the app before restore, verify on a copy, and keep passphrases separate.
- Need help: download the Support bundle and include app version, steps, and logs.

## Notes
- Keep placeholders unchanged (e.g. `{name}`, `{username}`, `{version}`).
- Preserve meaning of security/licensing text; avoid ambiguous wording.
