# TODO (manual / external)

These items are intentionally left for manual/external follow-up.

## i18n / UX
- [ ] **Native-speaker translation review** for `el,de,nl,da,fr` using `docs/i18n-review-checklist.md`; verify licensing/security/support wording, placeholders, and UI fit, including the new Booking.com email-import labels.
- [ ] **Booking.com paste-helper template maintenance**: fixtures cover standard and updated confirmations in English and Greek only. If Booking.com changes labels or adds templates, update `frontend/src/bookingEmailImport.js` and `frontend/tests/bookingEmailImport.test.js`.
- [ ] **Add screenshots/GIFs** to README/docs (login, license activation, booking calendar, reports, support bundle/download).

## Release / operations
Use [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for the full release gate.
- [ ] **Real anonymized DB upgrade smoke** before releases: backup copy -> migrate -> login/license/booking/report smoke. Confirm a V1-baselined/current schema; pre-V1 DBs are unsupported and must be exported/reimported or upgraded outside the supported path.
- [x] **Production keypair/signing process**: production Ed25519 keypair generated; `APP_LICENSE_PUBLIC_KEY` is required in `prod`, private key stays outside the repo, and the signing workflow is documented in `README.md`.
- [x] **Backup/restore drill** on a staging copy: encrypted backup, restore, app startup, key data/report checks passed on 2026-11-25 (v12/current schema copy). Repeat per release using `docs/RELEASE_CHECKLIST.md`; the V1-baseline upgrade smoke remains separate and pending.
- [x] **USB test kit docs/scripts**: `usb-kit/` assembles already-built portable packages and one activated `bookings.db` onto a USB. Real target-OS smoke remains tracked in `docs/RELEASE_CHECKLIST.md`.
- [ ] **Cross-OS package limitation — do not forget**: Windows and macOS portable archives cannot be generated on this Linux host. Build Windows with `install/package.ps1 portable` on Windows 11 x86-64; build macOS with `install/package.sh portable` once on Intel x86-64 and once on Apple Silicon arm64. The staged kit has all four `packages/` folders, but Windows/macOS are empty until those archives are built and copied in; `verify-kit.sh` will report them missing until then.
- [ ] **USB demo license is throwaway**: the staged `bookings.db` is activated with demo customer details and install ID `7c597bff-6d90-4fb9-a983-8ce81cca02aa`. It is for internal/demo USB testing only. Generate a fresh install ID and signed license for the new empty production DB; never ship this demo activation to a customer.
- [x] **Linux x86-64 portable package smoke**: Fedora 45 launcher started from a fresh prod database; login, backup/restore wrappers, production-signed activation, bookings/resources/report, app-info, and support-bundle build ID/schema/license state verified. Ubuntu, `.deb`, Windows, and macOS smoke still pending.

## Security / quality (optional next)
- [ ] **Review Actuator exposure** for deployment needs; keep admin-only or add probe-safe health endpoint rules as required.
- [ ] **Expand domain error codes** if frontend/API consumers need stable codes beyond current `ApiError.code`.
- [ ] **Broader E2E coverage** (Playwright): licensed flow, booking CRUD, report export, role restrictions; the basic login smoke is already wired into CI with a Chromium install.

## Notes
- Dev/test license bypass is enabled by default dev profile (`app.license.bypass=true`). Production must use `APP_LICENSE_BYPASS=false` / prod profile.
- Support bundle endpoint: `/api/v1/support-bundle` (authenticated, best-effort redaction; review before sharing).
