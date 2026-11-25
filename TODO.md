# TODO (manual / external)

These items are intentionally left for manual/external follow-up.

## i18n / UX
- [ ] **Native-speaker translation review** for `el,de,nl,da,fr` using `docs/i18n-review-checklist.md`; verify licensing/security/support wording, placeholders, and UI fit.
- [ ] **Add screenshots/GIFs** to README/docs (login, license activation, booking calendar, reports, support bundle/download).

## Release / operations
Use [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for the full release gate.
- [ ] **Real anonymized DB upgrade smoke** before releases: backup copy -> migrate -> login/license/booking/report smoke. Confirm a V1-baselined/current schema; pre-V1 DBs are unsupported and must be exported/reimported or upgraded outside the supported path.
- [x] **Production keypair/signing process**: production Ed25519 keypair generated; `APP_LICENSE_PUBLIC_KEY` is required in `prod`, private key stays outside the repo, and the signing workflow is documented in `README.md`.
- [x] **Backup/restore drill** on a staging copy: encrypted backup, restore, app startup, key data/report checks passed on 2026-11-25 (v12/current schema copy). Repeat per release using `docs/RELEASE_CHECKLIST.md`; the V1-baseline upgrade smoke remains separate and pending.

## Security / quality (optional next)
- [ ] **Review Actuator exposure** for deployment needs; keep admin-only or add probe-safe health endpoint rules as required.
- [ ] **Expand domain error codes** if frontend/API consumers need stable codes beyond current `ApiError.code`.
- [ ] **Broader E2E coverage** (Playwright): licensed flow, booking CRUD, report export, role restrictions; the basic login smoke is already wired into CI with a Chromium install.

## Notes
- Dev/test license bypass is enabled by default dev profile (`app.license.bypass=true`). Production must use `APP_LICENSE_BYPASS=false` / prod profile.
- Support bundle endpoint: `/api/v1/support-bundle` (authenticated, best-effort redaction; review before sharing).
