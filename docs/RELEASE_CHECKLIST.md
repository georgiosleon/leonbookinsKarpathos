# Release checklist

Use this before tagging or shipping a production package. Automated checks are
necessary but not sufficient; the manual sign-offs below are release gates.

## 1. Automated gate

Run on a clean working tree from the repository root:

```sh
./mvnw -o -Pquality verify
export PATH="$PWD/target/node/node:$PATH"
(cd frontend && npm test && npm run build && node scripts/check-docs-smoke.js)
git diff --check
```

For the browser smoke test, start the built JAR with a throwaway database and a
loopback port, then run the Playwright smoke from `frontend/`:

```sh
PLAYWRIGHT_BASE_URL=http://127.0.0.1:8081 npm run test:e2e
```

Record the results in the release notes. A red automated gate blocks shipping.

## 2. Licensing and packaging sign-off

- [ ] Build with the `prod` profile, `APP_LICENSE_BYPASS=false`, and an explicit
      production `APP_LICENSE_PUBLIC_KEY`. The launcher bakes in the public key
      and build ID; no development key fallback is allowed in production.
- [ ] Keep the production private key outside the repository and use
      `tools/LicenseSigner.java` only from a secure path to sign customer keys.
- [ ] Produce one artifact per supported OS, with matching Intel/Apple Silicon
      builds for macOS. Do not ship per-customer code forks.
- [ ] Confirm the support policy and OS matrix in `README.md`,
      `install/README.md`, and in-app help still match the shipped package.

## 3. Backup/restore drill

Run this on a staging copy, never on the only copy of a customer database.
Stop the application before restoring.

```sh
java -jar leonbookinsKarpathos-<version>.jar db backup \
  --database ./bookings.db \
  --output ./backups/bookings.sqlite3.enc --encrypt

java -jar leonbookinsKarpathos-<version>.jar db restore \
  --database ./bookings.db \
  --input ./backups/bookings.sqlite3.enc \
  --backup-dir ./backups/pre-restore
```

The launcher equivalents are `LeonBookings backup [file]` and
`LeonBookings restore [file]`. Verify all of the following:

- [ ] Backup asks for a passphrase of at least 12 characters twice.
- [ ] Restore with a wrong passphrase fails and leaves the database unchanged.
- [ ] Restore creates a pre-restore copy under the backup directory.
- [ ] Row counts and row contents for bookings, resources, users, and Flyway
      history match the source copy.
- [ ] The app starts from the restored database; login, booking list, resource
      list, and a report for a known date range return the expected data.

| Date | Schema checked | Result | Sign-off |
|---|---|---|---|
| 2026-11-25 | v12/current copy | Local drill passed: encrypted backup, restore, logical content match, startup/login/bookings/resources/report smoke | Staging copy only |

This local drill does **not** replace the anonymized V1-baseline upgrade smoke
below.

## 4. Upgrade smoke

- [ ] Use an anonymized, V1-baselined/current schema copy.
- [ ] Start the release build against the copy and let Flyway finish.
- [ ] Verify login/license state, booking list, resource list, and report output.
- [ ] Confirm no pre-V1 database is accepted on the supported upgrade path;
      pre-V1 data must be exported/reimported or upgraded outside support.

## 5. Platform package smoke

| Platform | Required check |
|---|---|
| Windows 11 x86-64 | Launch from terminal; verify loopback bind, license gate, backup/restore wrapper, and build ID in support bundle. Note SmartScreen warning for unsigned builds. |
| Linux x86-64 | Launch portable archive and `.deb` package where applicable; verify the same checks. |
| macOS 13+ | Verify separate Intel x86-64 and Apple Silicon arm64 packages; note signing/notarization requirement for public distribution. |

For a multi-OS demo/test USB, use [usb-kit/README.md](../usb-kit/README.md). It
assembles the portable packages and one activated `bookings.db`; it is not an
ISO or a bootable image. Packages cannot be cross-built: Windows and macOS
archives must be produced on their matching OS, with separate Intel x86-64 and
Apple Silicon arm64 builds.

The Linux x86-64 portable archive requires glibc 2.38 or newer (checked with
`objdump -T` on the bundled runtime; `libjli.so`, `libjvm.so`, `libjava.so`, and
`libawt.so` reference `GLIBC_2.38`). It was built and smoke-tested on Fedora 45
(glibc 2.44). Ubuntu 24.04 (glibc 2.39) should run it; Ubuntu 22.04 (glibc 2.35)
and older cannot. If older Ubuntu must be supported, rebuild the package on an
older base system. A desktop install also provides the X11 libraries used only
by the optional browser auto-open; on headless systems open the URL manually.

| Date | Platform | Result | Sign-off |
|---|---|---|---|
| 2026-11-25 | Fedora 45 x86-64 portable `tar.gz` | Fresh prod launcher startup, login 200, launcher backup/restore wrappers, production-signed activation, bookings/resources/report/app-info/support-bundle 200, build ID/schema/license state verified | Local smoke only; Ubuntu, `.deb`, Windows, and macOS still pending |

## 6. Native-speaker translation review

Complete the review in [i18n-review-checklist.md](i18n-review-checklist.md) for
`el,de,nl,da,fr`. Check licensing/security/support wording, placeholders, and
UI fit. Automated translation counts only prove keys exist; they do not prove
the wording is acceptable.

## 7. Support readiness

- [ ] Review a generated support bundle before sharing. Log masking is
      best-effort; install IDs, customer names, or other personal data may remain.
- [ ] Confirm the build ID, schema version, license state, and deployment type
      in `app-info.json` match the shipped artifact.
- [ ] Publish the current/previous release support policy and the 12-month
      updates/setup-help boundary with the sale listing.

## 8. Go/no-go

Only tag and ship when the automated gate is green, every applicable sign-off
above is filled, and no private key or customer data is in the repository.
