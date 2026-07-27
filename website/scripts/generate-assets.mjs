import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

const PROJECT_DIRECTORY = resolve(import.meta.dirname, "..");
const FAVICON_PATH = resolve(PROJECT_DIRECTORY, "public", "favicon.svg");
const SOCIAL_PATH = resolve(PROJECT_DIRECTORY, "public", "social", "jdoor-preview.png");
const MANIFEST_PATH = resolve(PROJECT_DIRECTORY, "public", "social", "manifest.json");
const CHECK_ONLY = process.argv.includes("--check");
const ICONS = Object.freeze([
  { name: "jdoor-180.png", size: 180 },
  { name: "jdoor-192.png", size: 192 },
  { name: "jdoor-512.png", size: 512 }
]);

function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function validatePng(buffer, width, height, label) {
  assert(buffer.subarray(1, 4).toString("ascii") === "PNG", `${label} must be a PNG.`);
  assert(
    buffer.readUInt32BE(16) === width && buffer.readUInt32BE(20) === height,
    `${label} must be ${width} × ${height}.`
  );
}

function createSocialSource(favicon) {
  const icon = `data:image/svg+xml;base64,${favicon.toString("base64")}`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
  <defs>
    <style>
      .sans { font-family: "Segoe UI", Arial, sans-serif; }
      .mono { font-family: Consolas, monospace; }
    </style>
    <pattern id="grid" width="56" height="56" patternUnits="userSpaceOnUse">
      <path d="M56 0H0V56" fill="none" stroke="#F4F1EA" stroke-opacity=".045"/>
    </pattern>
  </defs>
  <rect width="1200" height="630" fill="#0B0D10"/>
  <rect width="1200" height="630" fill="url(#grid)"/>
  <rect x="32" y="32" width="1136" height="566" rx="28" fill="none" stroke="#303842"/>
  <image href="${icon}" x="72" y="65" width="96" height="96"/>
  <text x="190" y="118" class="sans" font-size="34" font-weight="700" letter-spacing="-1.5" fill="#F4F1EA">JDoor Assist</text>
  <text x="192" y="145" class="mono" font-size="12" letter-spacing="1.4" fill="#A7AFBA">CONSENT-FIRST REMOTE ASSISTANCE</text>
  <rect x="72" y="205" width="136" height="30" rx="15" fill="#3A2118" stroke="#FF6B35" stroke-opacity=".55"/>
  <text x="140" y="225" class="mono" font-size="11" text-anchor="middle" letter-spacing="1.2" fill="#FF9A74">PRE-RELEASE</text>
  <text x="72" y="322" class="sans" font-size="69" font-weight="700" letter-spacing="-4.2" fill="#F4F1EA">Remote help,</text>
  <text x="72" y="397" class="sans" font-size="69" font-weight="700" letter-spacing="-4.2" fill="#FF6B35">with consent visible.</text>
  <text x="76" y="454" class="sans" font-size="21" fill="#A7AFBA">Verified pairing · local approval · view-only by default</text>
  <line x1="72" y1="520" x2="700" y2="520" stroke="#303842"/>
  <text x="72" y="556" class="mono" font-size="12" letter-spacing="1.2" fill="#A7AFBA">TRUSTED LOCAL NETWORKS / NO BROWSER SESSION</text>
  <rect x="754" y="64" width="380" height="502" rx="24" fill="#15191F" stroke="#303842"/>
  <text x="788" y="103" class="mono" font-size="11" letter-spacing="1.2" fill="#A7AFBA">A SESSION STAYS HUMAN-GATED</text>
  <line x1="802" y1="154" x2="802" y2="458" stroke="#303842" stroke-width="2"/>
  <rect x="792" y="150" width="20" height="20" fill="#FF6B35"/>
  <text x="838" y="166" class="sans" font-size="20" font-weight="700" fill="#F4F1EA">One-time pairing</text>
  <text x="838" y="190" class="sans" font-size="14" fill="#A7AFBA">Token + exact certificate pin</text>
  <rect x="792" y="252" width="20" height="20" fill="#FF6B35"/>
  <text x="838" y="268" class="sans" font-size="20" font-weight="700" fill="#F4F1EA">Code comparison</text>
  <text x="838" y="292" class="sans" font-size="14" fill="#A7AFBA">Both people verify the same host</text>
  <rect x="792" y="354" width="20" height="20" fill="#45C486"/>
  <text x="838" y="370" class="sans" font-size="20" font-weight="700" fill="#F4F1EA">Local approval</text>
  <text x="838" y="394" class="sans" font-size="14" fill="#A7AFBA">The host decides who enters</text>
  <rect x="792" y="456" width="20" height="20" fill="#45C486"/>
  <text x="838" y="472" class="sans" font-size="20" font-weight="700" fill="#F4F1EA">View-only start</text>
  <text x="838" y="496" class="sans" font-size="14" fill="#A7AFBA">Control stays explicit and revocable</text>
  <text x="1100" y="543" class="mono" font-size="11" text-anchor="end" letter-spacing="1.1" fill="#66717D">JDOOR.EJUPILABS.COM</text>
</svg>`;
}

const favicon = await readFile(FAVICON_PATH);
const faviconText = favicon.toString("utf8");
assert(faviconText.includes('id="door-frame"'), "Favicon must contain the JDoor frame.");
assert(faviconText.includes('id="consent-node"'), "Favicon must contain the consent node.");
const socialSource = createSocialSource(favicon);
const sourceSha256 = sha256(Buffer.concat([favicon, Buffer.from(socialSource)]));

if (CHECK_ONLY) {
  const manifest = JSON.parse(await readFile(MANIFEST_PATH, "utf8"));
  assert(manifest.sourceSha256 === sourceSha256, "Brand source changed. Run npm run generate:assets.");

  for (const icon of ICONS) {
    const buffer = await readFile(resolve(PROJECT_DIRECTORY, "public", "icons", icon.name));
    validatePng(buffer, icon.size, icon.size, icon.name);
    assert(manifest.outputs?.[icon.name] === sha256(buffer), `${icon.name} does not match its manifest.`);
  }

  const social = await readFile(SOCIAL_PATH);
  validatePng(social, 1200, 630, "jdoor-preview.png");
  assert(
    manifest.outputs?.["jdoor-preview.png"] === sha256(social),
    "jdoor-preview.png does not match its manifest."
  );
  console.log("JDoor favicon, install icons and 1200 × 630 social preview are valid.");
} else {
  const { default: sharp } = await import("sharp");
  const outputs = {};
  await mkdir(resolve(PROJECT_DIRECTORY, "public", "icons"), { recursive: true });
  await mkdir(dirname(SOCIAL_PATH), { recursive: true });

  for (const icon of ICONS) {
    const buffer = await sharp(favicon)
      .resize(icon.size, icon.size)
      .png({ compressionLevel: 9, adaptiveFiltering: true })
      .toBuffer();
    validatePng(buffer, icon.size, icon.size, icon.name);
    await writeFile(resolve(PROJECT_DIRECTORY, "public", "icons", icon.name), buffer);
    outputs[icon.name] = sha256(buffer);
  }

  const social = await sharp(Buffer.from(socialSource))
    .png({ compressionLevel: 9, adaptiveFiltering: true })
    .toBuffer();
  validatePng(social, 1200, 630, "jdoor-preview.png");
  await writeFile(SOCIAL_PATH, social);
  outputs["jdoor-preview.png"] = sha256(social);

  await writeFile(
    MANIFEST_PATH,
    `${JSON.stringify(
      {
        sourceSha256,
        palette: {
          background: "#0B0D10",
          surface: "#15191F",
          text: "#F4F1EA",
          action: "#FF6B35",
          consent: "#45C486"
        },
        outputs,
        width: 1200,
        height: 630
      },
      null,
      2
    )}\n`
  );
  console.log("Generated JDoor icons and social preview.");
}
