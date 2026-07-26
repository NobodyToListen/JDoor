# Threat model

## Goal

Allow one person to show a desktop to one trusted helper on a local network, with authentication,
human consent, encrypted transport, and an immediate local stop control.

## Protected assets

- screen contents;
- pairing token and certificate fingerprint;
- integrity of pointer and keyboard input;
- host availability;
- viewer and host network addresses;
- audit records.

## Trust assumptions

- The host’s operating system and JVM are not already compromised.
- The viewer receives the complete pairing link through a trusted out-of-band channel.
- Both users can compare the short verification code.
- The host understands that approved screen frames reveal anything visible on the primary
  display.
- The direct network path may be observed or modified by an attacker.

## Threats and controls

| Threat | Control |
|---|---|
| Passive network capture | TLS 1.2/1.3 |
| Active man-in-the-middle | Advertised-host endpoint identification plus exact SHA-256 certificate pin carried out of band |
| Pairing token guessing | 128 bits of entropy, ten-minute expiry, constant-time comparison, generation-bound reservation, per-address limiting |
| Replayed link | Single-use token rotated after a session |
| Unwanted viewer | Local modal approval with device name, address, and verification code |
| Unwanted input | View-only default, explicit host toggle, immediate revoke and key/button release |
| Malformed protocol data | Version/type checks, strict size/range/UTF-8 validation, and bounded JPEG metadata-first decode |
| Memory/disk disclosure through logs | Tokens, pixels, commands, and keystrokes are never logged; files have size and retention caps |
| Stuck input after failure | Tracked pressed state released on revoke, disconnect, and shutdown |
| Resource exhaustion | One active viewer, per-address pending-handshake caps, absolute pre-auth deadline, bounded queues, frames, UI history, and audit files |
| Cross-session permission race | Control state, capture task, and cleanup are owned by one connection generation |

## Explicit non-goals

JDoor Assist does not provide:

- internet relay, NAT traversal, or public-server hardening;
- accounts, organizations, or centralized identity;
- unattended or background access;
- persistence or auto-start;
- shell/command execution;
- file transfer, clipboard synchronization, or webcam/microphone access;
- privilege escalation or security-control bypass;
- covert operation or evasion;
- protection when either endpoint is already compromised.

## Residual risk

- Anyone who obtains the unconsumed full link may request access; the local approval and code
  comparison remain mandatory.
- A malicious approved viewer can observe the shared screen and, while control is enabled, act
  with the host user’s desktop privileges.
- TLS identity is ephemeral and self-signed. Its SAN is bound to the stable advertised host, while
  pinning authenticates the exact pairing link rather than a legal person or long-lived device.
- The primary-display stream may expose notifications or secrets the host did not intend to
  show. Users should close sensitive material before approval.
- Audit files inherit the local account’s filesystem protections and are not cryptographically
  tamper-evident.
- Unsigned community packages may be modified after download unless the published checksum and
  provenance are verified.

## Review triggers

Repeat threat modeling before adding any relay, persistent identity, unattended access, file or
clipboard transfer, audio/video capture, multi-viewer mode, discovery broadcast, or automatic
firewall configuration.
