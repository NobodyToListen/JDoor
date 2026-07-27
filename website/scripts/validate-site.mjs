import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const PROJECT_DIRECTORY = resolve(import.meta.dirname, "..");
const PUBLIC_DIRECTORY = resolve(PROJECT_DIRECTORY, "public");

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const [html, notFound, styles, runtime, favicon, manifestRaw, robots, sitemap, security, preview] =
  await Promise.all([
    readFile(resolve(PUBLIC_DIRECTORY, "index.html"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "404.html"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "styles.css"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "main.js"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "favicon.svg"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "site.webmanifest"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "robots.txt"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "sitemap.xml"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, ".well-known", "security.txt"), "utf8"),
    readFile(resolve(PUBLIC_DIRECTORY, "social", "jdoor-preview.png"))
  ]);

assert((html.match(/<h1\b/g) ?? []).length === 1, "Homepage must have exactly one h1.");
assert(html.includes("<main"), "Homepage must have a main landmark.");
assert(html.includes('href="#main-content"'), "Homepage must provide a skip link.");
assert(
  [...html.matchAll(/<section[^>]+id="(principles|flow|security|privacy|boundaries|status)"/g)]
    .map(([, id]) => id)
    .join(",") === "principles,flow,security,privacy,boundaries,status",
  "Homepage sections must follow the product narrative."
);
assert(html.includes("Pre-release"), "Homepage must state the pre-release status.");
assert(/view-only(?: sessions)? by default/i.test(html), "Homepage must state the view-only default.");
assert(html.includes("This website does not start or join remote sessions"), "Homepage must reject a browser demo implication.");
assert(html.includes("128-bit") && html.includes("10 minutes"), "Homepage must preserve pairing-token evidence.");
assert(html.includes("TLS 1.2/1.3"), "Homepage must preserve the transport claim.");
assert(html.includes("5 MiB") && html.includes("30 days"), "Homepage must preserve the audit limits.");
assert(html.includes("NobodyToListen"), "Homepage must preserve original project attribution.");
assert(!/<form\b/i.test(html), "Homepage must not imply a server-backed form.");
assert(!/<script[^>]+src="https?:/i.test(html), "Homepage must not load third-party scripts.");
assert(!/<link[^>]+href="https?:[^>]+stylesheet/i.test(html), "Homepage must not load third-party styles.");
assert(!/download now|start session|join session online/i.test(html), "Homepage must not present a misleading live action.");
assert(
  /aria-controls="site-navigation"/.test(html) &&
    /<nav[\s\S]*?id="site-navigation"[\s\S]*?data-site-nav/.test(html),
  "Mobile navigation control must reference the navigation landmark."
);

const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
assert(jsonLdMatch, "Homepage must include JSON-LD.");
const structuredData = JSON.parse(jsonLdMatch[1]);
assert(structuredData["@type"] === "SoftwareApplication", "JSON-LD must describe a software application.");
assert(structuredData.name === "JDoor Assist", "JSON-LD has the wrong product name.");
assert(structuredData.softwareVersion === "Pre-release", "JSON-LD must preserve pre-release status.");
assert(structuredData.codeRepository === "https://github.com/NobodyToListen/JDoor", "JSON-LD has the wrong source repository.");

assert(notFound.includes('content="noindex, follow"'), "404 page must be noindex.");
assert((notFound.match(/<h1\b/g) ?? []).length === 1, "404 page must have exactly one h1.");
assert(styles.includes("@media (prefers-reduced-motion: reduce)"), "Styles must respect reduced motion.");
assert(styles.includes("@media (forced-colors: active)"), "Styles must support forced colors.");
assert(styles.includes("@media (max-width: 720px)"), "Styles must provide a compact layout.");
assert(runtime.includes('aria-expanded'), "Mobile navigation must keep aria-expanded in sync.");
assert(!/\bfetch\s*\(/.test(runtime), "Browser runtime must not make network requests.");

assert(favicon.includes('id="door-frame"'), "Favicon must contain the JDoor frame.");
assert(favicon.includes('id="consent-node"'), "Favicon must contain the consent node.");
assert(favicon.includes("#FF6B35") && favicon.includes("#45C486"), "Favicon must use JDoor action and consent colors.");

const manifest = JSON.parse(manifestRaw);
assert(manifest.short_name === "JDoor", "Web manifest has the wrong short name.");
assert(
  manifest.icons.some((icon) => icon.sizes === "192x192") &&
    manifest.icons.some((icon) => icon.sizes === "512x512"),
  "Web manifest must expose 192px and 512px icons."
);

assert(robots.includes("https://jdoor.ejupilabs.com/sitemap.xml"), "robots.txt has the wrong sitemap.");
assert(sitemap.includes("<loc>https://jdoor.ejupilabs.com/</loc>"), "Sitemap is missing the canonical homepage.");
assert(security.includes("https://github.com/NobodyToListen/JDoor/security/advisories/new"), "security.txt must use private reporting.");
assert(security.includes("Canonical: https://jdoor.ejupilabs.com/.well-known/security.txt"), "security.txt has the wrong canonical URL.");
const expires = security.match(/^Expires:\s*(.+)$/m)?.[1];
assert(Boolean(expires) && Date.parse(expires) > Date.now(), "security.txt must have a future expiry.");

assert(preview.subarray(1, 4).toString("ascii") === "PNG", "Social preview must be a PNG.");
assert(preview.readUInt32BE(16) === 1200 && preview.readUInt32BE(20) === 630, "Social preview must be 1200 × 630.");

console.log("Validated JDoor product narrative, accessibility, metadata, static assets and security disclosures.");
