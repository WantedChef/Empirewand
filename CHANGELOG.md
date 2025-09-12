# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project adheres to Semantic Versioning.

## [Unreleased]
- Add more spell test coverage to maintain >=80% JaCoCo threshold.
- Improve config migrations for `config.yml` and `spells.yml`.

## [1.1.1] - 2025-09-12
### Added
- JaCoCo coverage with 80% minimum and CI reports.
- Shaded and relocated bStats; metrics disabled by default via `config.yml`.
- CI matrix (Linux + Windows), artifacts for tests, coverage, Checkstyle, SpotBugs.
- Release workflow publishes only shaded JAR with SHA256 checksums.

### Changed
- `plugin.yml` authors updated to `ChefWanted`.
- README updated with supported versions (Paper 1.20.6, Java 21), install/upgrade, telemetry notes.

### Fixed
- Stability improvements in build and packaging.

[Unreleased]: https://github.com/WantedChef/Empirewand/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/WantedChef/Empirewand/releases/tag/v1.1.1
