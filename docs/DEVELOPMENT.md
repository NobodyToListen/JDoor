# Development guide

## Toolchain

- JDK 21
- Maven 3.9.16 through the checked-in wrapper
- Git

Run the complete local gate:

```bash
./mvnw clean verify
```

Useful focused commands:

```bash
./mvnw test
./mvnw -Dtest=HostViewerIntegrationTest test
java -jar target/jdoor-assist-1.0.0-all.jar --help
```

Tests are headless. The integration suite still opens real loopback TLS sockets, pins the
ephemeral certificate, rejects a wrong token, streams a real JPEG frame, verifies view-only
behavior, then exercises permission-gated pointer and key messages. Deterministic latch-based
tests also cover cross-session control races, blocked writes, pre-auth deadlines, candidate
shutdown, connection cancellation, input coalescing, and token-generation rotation.

## Design rules

- Keep Swing work on the Event Dispatch Thread and network/capture work off it.
- Inject `ScreenSource`, `RemoteInputController`, `Clock`, and randomness at boundaries.
- Validate records on construction so an invalid state cannot enter session logic.
- Treat every byte from the network as untrusted.
- Cap collection/string/frame sizes before allocation.
- Treat declared media dimensions as untrusted and verify them against the decoded raster.
- Bind permission, background tasks, and cleanup to one connection generation.
- Use constant-time comparison for authentication material.
- Redact secrets by construction; `SessionToken.toString()` must remain redacted.
- Close sockets to interrupt blocking reads and release every pressed input during cleanup.

## Protocol changes

Protocol version 1 is intentionally small. A compatible addition needs:

1. a new message type with a unique code;
2. constructor-level validation;
3. codec round-trip and malformed-frame tests;
4. direction checks in host/viewer loops;
5. updated architecture and threat-model documentation.

Breaking changes require a protocol-version increment and an explicit rejection path.

## CI repository settings

The cross-platform build, CodeQL analysis, SBOM, and release gates work without repository
configuration. To make dependency review a blocking pull-request check, a repository maintainer
must enable GitHub's Dependency graph and set the Actions variable
`DEPENDENCY_REVIEW_ENABLED=true`. The workflow remains gated until both are available so an
unsupported API cannot hide the build and security checks that do run.

## Manual desktop smoke test

1. Run two JDoor Assist processes on one machine.
2. Start hosting in the first and join via `127.0.0.1` in the generated link if needed.
3. Confirm the viewer cannot connect before local approval.
4. Confirm the verification code matches.
5. Confirm frames are visible and input does nothing in view-only mode.
6. Enable control, test pointer/key input, then disable it and confirm input stops immediately.
7. Disconnect both ways and verify a fresh pairing link appears.
8. Inspect the audit file and confirm it contains no token or typed content.

Screen recording and synthetic input can require OS permissions. Test on a non-sensitive desktop.
