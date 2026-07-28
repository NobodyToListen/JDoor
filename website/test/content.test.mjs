import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const [html, styles, runtime, assetGenerator, wrangler, worker, handler] = await Promise.all([
  readFile(new URL("../public/index.html", import.meta.url), "utf8"),
  readFile(new URL("../public/styles.css", import.meta.url), "utf8"),
  readFile(new URL("../public/main.js", import.meta.url), "utf8"),
  readFile(new URL("../scripts/generate-assets.mjs", import.meta.url), "utf8"),
  readFile(new URL("../wrangler.jsonc", import.meta.url), "utf8"),
  readFile(new URL("../worker/index.ts", import.meta.url), "utf8"),
  readFile(new URL("../worker/handler.ts", import.meta.url), "utf8")
]);

test("the product narrative leads with evidence and keeps permission ahead of origin", () => {
  const interfaceIndex = html.indexOf('id="interface"');
  const storyIndex = html.indexOf('id="story"');
  const flowIndex = html.indexOf('id="flow"');
  const decisionsIndex = html.indexOf('id="decisions"');
  const trustIndex = html.indexOf('id="trust"');
  const boundaryIndex = html.indexOf('id="boundaries"');
  assert(interfaceIndex > 0);
  assert(storyIndex > 0);
  assert(decisionsIndex > 0);
  assert(interfaceIndex < flowIndex);
  assert(flowIndex < storyIndex);
  assert(storyIndex < decisionsIndex);
  assert(decisionsIndex < trustIndex);
  assert(flowIndex < boundaryIndex);
  assert.match(html, /co-created by Djenis Ejupi and NobodyToListen as a school experiment/iu);
  assert.match(html, /view-only sessions by default/iu);
  assert.match(html, /host enables it for that session/iu);
  assert.match(html, /revoke/iu);
});

test("technology choices name the alternative and accepted cost", () => {
  assert.match(html, /Java 21 \+ Swing/);
  assert.match(html, /Direct trusted-LAN connection/);
  assert.match(html, /Ephemeral TLS \+ exact pin/);
  assert.match(html, /Bounded protocol, view first/);
  assert.equal((html.match(/<dt>Instead of<\/dt>/g) ?? []).length, 4);
  assert.equal((html.match(/<dt>Accepted cost<\/dt>/g) ?? []).length, 4);
  assert.match(html, /pin identifies the endpoint—not the person/iu);
  assert.match(html, /No NAT traversal/iu);
});

test("scope is explained through concrete fits and explicit alternatives", () => {
  assert.match(html, /Helping family on the same trusted home network/iu);
  assert.match(html, /classroom or lab workstation/iu);
  assert.match(html, /colleague in a small office/iu);
  assert.match(html, /Choose something else/iu);
  assert.match(html, /fleet management/iu);
});

test("real product surfaces support the consent narrative", () => {
  assert.match(html, /jdoor-launcher\.avif/);
  assert.match(html, /jdoor-launcher\.webp/);
  assert.match(html, /jdoor-local-approval\.avif/);
  assert.match(html, /jdoor-local-approval\.webp/);
  assert.match(html, /actual desktop surfaces from the 1\.0\.0 codebase/iu);
  assert.match(html, /endpoint is redacted here/iu);
  assert.doesNotMatch(html, /192\.168\.\d+\.\d+/);
});

test("the published source status is precise about distribution", () => {
  assert.match(html, /Source 1\.0\.0/);
  assert.match(html, /softwareVersion": "1\.0\.0"/);
  assert.match(html, /no signed\s+installer or tagged GitHub release/iu);
  assert.doesNotMatch(html, /pre-release/iu);
  assert.match(assetGenerator, /REMOTE ASSISTANCE \/ SOURCE 1\.0\.0/);
  assert.doesNotMatch(assetGenerator, /PRE-RELEASE/iu);
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
  assert.doesNotMatch(html, /every viewer is verified/iu);
  assert.match(html, /software checks the endpoint.*host decides the person/isu);
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

test("mobile navigation locks background interaction and traps keyboard focus", () => {
  assert.match(styles, /html\[data-menu-open\]\s*\{[^}]*overflow:\s*hidden/s);
  assert.match(runtime, /target\.inert = open/);
  assert.match(runtime, /event\.key === "Tab"/);
});

test("mutable shell assets use revisioned URLs", () => {
  assert.match(html, /href="\/styles\.css\?v=\d+"/);
  assert.match(html, /src="\/main\.js\?v=\d+"/);
  assert.match(html, /href="\/site\.webmanifest\?v=\d+"/);
});
