# Contributing

Thank you for helping improve JDoor Assist.

## Principles

Every change must preserve these invariants:

- screen sharing is visible on the host;
- every viewer is authenticated and locally approved;
- control starts disabled and is immediately revocable;
- protocol input is bounded and treated as untrusted;
- logs never contain pairing tokens, screen pixels, or keystrokes;
- no stealth, persistence, shell execution, webcam access, or privilege elevation.

Changes that weaken those boundaries will not be accepted.

## Development

1. Install JDK 21.
2. Create a focused branch.
3. Run `./mvnw clean verify`.
4. Update tests and documentation with behavior changes.
5. Keep commits reviewable and avoid generated files below `target/`.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for architecture and testing details.

## Pull requests

Describe the user-visible outcome, security impact, tests run, and any trade-offs. UI changes
should include keyboard and focus behavior. Protocol changes require a versioning and
backward-compatibility note.
