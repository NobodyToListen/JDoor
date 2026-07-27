import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const [html, styles, wrangler, worker, handler] = await Promise.all([
  readFile(new URL("../public/index.html", import.meta.url), "utf8"),
  readFile(new URL("../public/styles.css", import.meta.url), "utf8"),
  readFile(new URL("../wrangler.jsonc", import.meta.url), "utf8"),
  readFile(new URL("../worker/index.ts", import.meta.url), "utf8"),
  readFile(new URL("../worker/handler.ts", import.meta.url), "utf8")
]);

test("the product story keeps consent ahead of capability", () => {
  const consentIndex = html.indexOf('id="principles"');
  const flowIndex = html.indexOf('id="flow"');
  const boundaryIndex = html.indexOf('id="boundaries"');
  assert(consentIndex > 0);
  assert(consentIndex < flowIndex);
  assert(flowIndex < boundaryIndex);
  assert.match(html, /Every session\s+begins view-only/);
  assert.match(html, /control.*host.*enable/isu);
  assert.match(html, /revoke/iu);
});

test("the site is documentation, not a remote-control surface", () => {
  assert.match(html, /does not start or join remote sessions/iu);
  assert.doesNotMatch(html, /<button[^>]*>\s*(?:Start|Join|Connect)/iu);
  assert.doesNotMatch(html, /jdoor:\/\/[^<\s]+/iu);
  assert.doesNotMatch(html, /pairing token:\s*[A-Za-z0-9_-]+/iu);
});

test("security and privacy claims stay within repository evidence", () => {
  for (const claim of [
    /TLS 1\.2\/1\.3/,
    /128-bit/,
    /10 minutes/,
    /single-use/,
    /5 MiB/,
    /30 days/,
    /no telemetry/i,
    /trusted local network/i
  ]) {
    assert.match(html, claim);
  }

  assert.doesNotMatch(html, /zero risk|unhackable|military-grade|internet-ready|end-to-end encrypted/iu);
});

test("Cloudflare serves every asset through the security-header Worker", () => {
  const config = JSON.parse(wrangler);
  assert.equal(config.compatibility_date, "2026-07-27");
  assert.equal(config.workers_dev, false);
  assert.equal(config.preview_urls, false);
  assert.deepEqual(config.routes, [{ pattern: "jdoor.ejupilabs.com", custom_domain: true }]);
  assert.equal(config.assets.binding, "ASSETS");
  assert.equal(config.assets.not_found_handling, "404-page");
  assert.equal(config.assets.html_handling, "auto-trailing-slash");
  assert.equal(config.assets.run_worker_first, true);
  assert.match(worker, /satisfies ExportedHandler<Env>/);
  assert.match(handler, /env\.ASSETS\.fetch\(request\)/);
  assert.match(handler, /Content-Security-Policy/);
  assert.match(handler, /Strict-Transport-Security/);
  assert.doesNotMatch(
    handler,
    /^const\s+[A-Z][A-Z0-9_]*\s*=\s*new\s+Response\b/m,
    "Response instances must be created inside request handlers, not at module scope."
  );
  assert.doesNotMatch(
    `${worker}\n${handler}`,
    /passThroughOnException|Math\.random|cloudflare\.com\/client\/v4/
  );
});

test("layout contracts preserve readable type and touch targets", () => {
  assert.match(styles, /min-width:\s*2\.75rem/);
  assert.match(styles, /min-height:\s*2\.75rem/);
  assert.match(styles, /text-wrap:\s*balance/);
  assert.match(styles, /overflow-wrap:\s*anywhere/);
});
