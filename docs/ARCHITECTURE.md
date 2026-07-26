# Architecture

JDoor Assist is a direct, single-viewer desktop application. The host owns the screen, consent
decision, and control permission; the viewer has no persistent identity or unattended-access
path.

## Components

| Layer | Responsibility |
|---|---|
| `ui` | Visible host/viewer states, consent prompt, pairing UX, focus and input capture |
| `session` | Connection lifecycle, token consumption, approval, streaming, revocation, cleanup |
| `security` | Ephemeral certificate, SHA-256 pin, TLS contexts, 128-bit single-use token |
| `protocol` | Directional, versioned, size-bounded binary messages |
| `capture` | Primary-display capture, aspect-preserving resize, bounded JPEG encoding |
| `control` | Host-side input policy, coordinate mapping, pressed-input cleanup |
| `audit` | Size- and retention-bounded daily JSONL events with no tokens, pixels, or keystrokes |

The AWT-specific capture and input classes implement small interfaces. Session and protocol tests
therefore run headlessly with deterministic fakes while the production UI uses the OS desktop.

## Session lifecycle

1. The host generates an in-memory P-256 key pair and self-signed certificate.
2. A random 128-bit token and the certificate’s SHA-256 fingerprint enter the `jdoor://` link.
3. The viewer establishes TLS and trusts only the exact certificate fingerprint in that link.
4. The token and visible device name are sent inside TLS.
5. Pre-authentication sockets are capped per address and closed at an absolute deadline.
6. The host compares the token in constant time, reserves its exact generation, and prompts
   locally while both users can see the same short verification code.
7. Approval consumes only that reserved token generation. The host sends JPEG frames; control
   remains disabled.
8. The host may enable input for that exact session. Disabling control releases every pressed key
   and mouse button.
9. Disconnect closes the channel, cancels that session’s capture task, releases input, records an
   audit event, and rotates to a fresh token.

## Wire protocol

Every frame contains:

```text
magic:u32 | version:u16 | type:u8 | payloadLength:u32 | payload
```

The codec rejects unknown versions/types, negative or oversized lengths, invalid UTF-8, trailing
bytes, impossible dimensions, non-finite coordinates, and images above 6 MiB. The viewer checks
JPEG dimensions before raster allocation, caps decoded pixels, and requires the encoded dimensions
to match the declared protocol frame. Messages are directional: a viewer cannot send a screen frame
or permission state, and a host cannot send synthetic input.

Protocol version 1 intentionally supports screen frames, pointer/key input, permission state,
heartbeat, rejection, and graceful disconnect only. Adding file transfer, clipboard sync, or
other capabilities requires a separate threat-model update.

## Concurrency

- One daemon thread accepts sockets.
- A bounded-size worker pool handles handshakes and the active viewer read loop. A separate
  deadline scheduler closes incomplete pre-authentication sockets, including queued candidates.
- One scheduled thread captures and encodes frames at a bounded rate. Its task and control
  permission belong to one connection generation.
- One viewer thread reads host messages; a separate scheduled task sends heartbeats.
- A bounded viewer input dispatcher coalesces pointer movement and prioritizes release-all over
  stale queued input.
- `MessageChannel.send` is synchronized so frames, heartbeats, and permission updates cannot
  interleave.
- Network callbacks marshal Swing changes to the Event Dispatch Thread.

All executors are process-local and stopped with the session. Closing either socket is the
authoritative way to interrupt blocking reads or writes; forced cleanup never waits to send a
graceful goodbye first.

## Distribution

Maven creates:

- `target/jdoor-assist-1.0.0.jar` — thin application JAR;
- `target/jdoor-assist-1.0.0-all.jar` — runnable shaded JAR;
- `target/jdoor-assist-sbom.json` — CycloneDX dependency inventory;
- `target/site/jacoco/` — coverage report.

`jpackage` scripts create an unsigned platform app image. Release automation builds on Windows,
macOS, and Linux; it does not cross-compile or claim platform signing.
