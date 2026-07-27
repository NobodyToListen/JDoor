# JDoor Assist website

The official project website for [JDoor](https://jdoor.ejupilabs.com/). Its editorial structure
connects the project’s 2022 school origin to the later security and product rebuild, then explains
one session, the division between software checks and human approval, deliberate limits and the
current pre-release status. It never presents a browser-based remote session.

## Architecture

- `public/` contains the dependency-free HTML, CSS, browser JavaScript and generated brand assets.
- `worker/` is a small Cloudflare Worker that serves static assets with one immutable security
  policy, cache rules and a real custom 404 response.
- `scripts/` generates deterministic PNG assets, validates the product narrative and runs a local
  static preview.
- `test/` and `e2e/` protect factual claims, responsive behavior, accessibility and deployment
  configuration.
- `wrangler.jsonc` maps the Worker to the custom domain `jdoor.ejupilabs.com`.

The website cannot create, join or proxy JDoor sessions. All product interactions remain in the
desktop application.

## Local verification

Use Node.js 24 and npm 11:

```bash
npm ci
npm run check
```

The full check audits locked dependencies, verifies generated assets, regenerates Worker types,
type-checks the Worker, runs unit/content/browser tests, validates metadata and performs a
Wrangler deployment dry-run.

To inspect the site locally:

```bash
npm run preview
```

Then open `http://127.0.0.1:4175`.

## Asset generation

`public/favicon.svg` is the canonical source for app icons. The same generator creates the
1200 × 630 social preview and records source/output hashes in `public/social/manifest.json`.

```bash
npm run generate:assets
npm run check:assets
```

## Deployment

After a reviewed change has passed CI, an authorized maintainer can deploy from this directory:

```bash
npm run deploy
```

Deployment requires Cloudflare credentials with access to the `ejupilabs.com` zone. The production
configuration disables `workers.dev` and preview URLs and serves only the configured custom
domain. Do not deploy unreviewed changes.
