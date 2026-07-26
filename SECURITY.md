# Security policy

## Supported versions

Only the latest `1.x` release receives security fixes.

## Report a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub’s private
**Report a vulnerability** flow in the repository Security tab. Include:

- affected version and operating system;
- whether the host, viewer, or both are affected;
- reproducible steps or a minimal proof of concept;
- impact and any known mitigations.

Never include a live pairing link, audit log containing personal data, or another person’s
screen capture. Maintainers will acknowledge a complete report within seven days and coordinate
disclosure after a fix is available.

## Security boundaries

JDoor Assist assumes the pairing link reaches the intended helper through a trusted channel. The
certificate fingerprint prevents a different TLS endpoint from impersonating that link’s host;
the token authorizes one connection attempt; the local prompt provides human consent.

This is not an internet-facing remote-management service. Public relays, unattended access,
accounts, persistence, file transfer, webcam capture, shell execution, and privilege elevation
are explicitly out of scope. See [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md).
