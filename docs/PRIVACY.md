# Privacy

JDoor Assist is local-first and has no telemetry, analytics, crash-report upload, account service,
or public relay.

## Data transmitted

During an approved direct session:

- JPEG frames of the primary display travel from host to viewer;
- pointer and keyboard event codes travel from viewer to host only when control is enabled;
- a device name, network metadata, heartbeats, and permission state support the session.

All of this travels inside the pinned TLS connection. JDoor Assist does not store screen frames or
input events.

## Data stored

The host writes a daily JSONL audit file below:

```text
~/.jdoor-assist/audit/
```

Events include timestamp, event type, remote network address, and a short lifecycle description.
They exclude pairing tokens, certificate private keys, screen pixels, typed content, and raw input
events. Each daily file is capped at 5 MiB, and application-owned logs older than 30 days are
removed during the next daily audit write. Ephemeral TLS private keys exist in memory only for the
host process lifetime.

Users may delete local audit files sooner, or archive them elsewhere when their retention
obligations require longer storage. No application server receives a copy.

## User responsibility

Obtain permission before sharing or viewing a screen. Avoid displaying personal or confidential
material unrelated to the support task. The host should disable notifications and close sensitive
applications before approving a viewer.
