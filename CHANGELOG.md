# Changelog

All notable changes to this project should be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses semantic version tags: `vMAJOR.MINOR.PATCH`.

## [Unreleased]

### Added
- Maven `quality` profile (JaCoCo, Checkstyle, PMD, SpotBugs, Enforcer) for opt-in verify checks.
- CodeQL workflow for Java and JavaScript/TypeScript analysis.
- Dependency review workflow for pull requests.
- GitHub governance files: CODEOWNERS and pull request template.
- Branch protection for `master`/`main` and `v*` tag protection ruleset.

### Changed
- GitHub Actions pinned to commit SHAs (Dependabot keeps updates).
- Desktop release workflow hardened for JDK/jmods and macOS bash compatibility.

## [2.0.1] - 2026-11-23

### Changed
- Bumped release version and validated release CI with dummy PR smoke test.

## [2.0.0] - 2026-11-23

### Changed
- Major version bump with booking model/API modernization (status enum, BigDecimal money, LocalDate dates, `resourceId`).
- Removed legacy API/env aliases and cleaned up deprecated paths.
