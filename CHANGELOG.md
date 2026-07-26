# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
Semantic Versioning.

## [Unreleased]

### Planned

- Signed installers when platform signing identities are available.
- User-tested screen-recording permission guidance for macOS and Wayland.
- Optional relay design only after an independent security review.

## [1.0.0] - 2026-07-26

### Added

- Consent-first launcher, host, and viewer interfaces.
- Direct TLS 1.2/1.3 transport with ephemeral ECDSA server certificates.
- Certificate-pinned, single-use pairing links and local viewer approval.
- View-only default with explicit, immediately revocable input control.
- Versioned binary protocol with payload, string, image, and coordinate limits.
- Authentication attempt limiting, pre-authentication deadlines, connection-scoped permissions,
  keepalive, deterministic cleanup, and a bounded JSONL audit trail.
- Deterministic concurrency and lifecycle tests for blocked writes, session replacement, slow
  clients, cancellation, and token rotation.
- Java 21 Maven build, integration tests, CI, CodeQL, SBOM, packaging, and release workflows.
- Architecture, privacy, threat-model, security, and contributor documentation.

### Removed

- Unauthenticated remote shell execution.
- Legacy UDP image fragmentation.
- Implicit keyboard/mouse control.
- “Backdoor” positioning, debug prints, IntelliJ GUI form dependency, and dead code.

[Unreleased]: https://github.com/NobodyToListen/JDoor/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/NobodyToListen/JDoor/releases/tag/v1.0.0
