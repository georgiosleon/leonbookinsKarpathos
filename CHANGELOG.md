# Changelog

All notable changes to this project should be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses semantic version tags: `vMAJOR.MINOR.PATCH`.

## [Unreleased]

## [2.0.2] - 2026-11-24

### Added
- English and Greek end-user manuals for the booking planner UI (`docs/USER_MANUAL.md`, `docs/USER_MANUAL_EL.md`).
- Onboarding notes covering architecture, operations, known issues, and owner decisions (`docs/ONBOARDING.md`).
- Maven `quality` profile (JaCoCo, Checkstyle, PMD, SpotBugs, Enforcer) for opt-in verify checks.
- CodeQL workflow for Java and JavaScript/TypeScript analysis.
- Dependency review workflow for pull requests.
- GitHub governance files: CODEOWNERS and pull request template.
- Branch protection for `master`/`main` and `v*` tag protection ruleset.

### Changed
- Booking save payload now includes `platformReservationId` and omits backend-derived `numOfNights`, `balance`, and `rentalTax`; client money helpers are display-only estimates.
- Bumped release version to `2.0.2`.
- GitHub Actions pinned to commit SHAs (Dependabot keeps updates).
- Desktop release workflow hardened for JDK/jmods and macOS bash compatibility.

## [2.0.1] - 2026-11-23

### Changed
- Bumped release version and validated release CI with dummy PR smoke test.

## [2.0.0] - 2026-11-23

### Changed
- Major version bump with booking model/API modernization (status enum, BigDecimal money, LocalDate dates, `resourceId`).
- Removed legacy API/env aliases and cleaned up deprecated paths.
