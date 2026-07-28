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
    <pattern id="grid" width="64" height="64" patternUnits="userSpaceOnUse">
      <path d="M64 0H0V64" fill="none" stroke="#0D100F" stroke-opacity=".055"/>
    </pattern>
  </defs>
  <rect width="1200" height="630" fill="#F4F1EA"/>
  <rect width="1200" height="630" fill="url(#grid)"/>
  <rect x="31" y="31" width="1138" height="568" fill="none" stroke="#0D100F"/>
  <rect x="31" y="31" width="10" height="568" fill="#EF5D32"/>

  <image href="${icon}" x="72" y="67" width="72" height="72"/>
  <text x="166" y="107" class="sans" font-size="31" font-weight="700" letter-spacing="-1.4" fill="#0D100F">JDoor</text>
  <text x="168" y="132" class="mono" font-size="11" letter-spacing="1.35" fill="#626761">REMOTE ASSISTANCE / SOURCE 1.0.0</text>

  <line x1="72" y1="184" x2="690" y2="184" stroke="#0D100F"/>
  <text x="72" y="212" class="mono" font-size="11" letter-spacing="1.25" fill="#BD4627">A 2022 SCHOOL PROJECT, REBUILT</text>
  <text x="72" y="297" class="sans" font-size="64" font-weight="700" letter-spacing="-3.8" fill="#0D100F">The person at the screen</text>
  <text x="72" y="370" class="sans" font-size="64" font-style="italic" letter-spacing="-3.6" fill="#BD4627">stays in charge.</text>
  <text x="76" y="421" class="sans" font-size="20" fill="#626761">Short-lived pairing · local approval · view-only start</text>

  <line x1="72" y1="500" x2="690" y2="500" stroke="#0D100F" stroke-opacity=".25"/>
  <text x="72" y="536" class="mono" font-size="11" letter-spacing="1.1" fill="#626761">TRUSTED LAN / TLS 1.2–1.3 / OPEN SOURCE</text>
  <text x="690" y="536" class="mono" font-size="11" text-anchor="end" letter-spacing="1.1" fill="#626761">JDOOR.EJUPILABS.COM</text>

  <rect x="754" y="68" width="372" height="486" fill="#FFFDF8" stroke="#0D100F"/>
  <text x="780" y="100" class="mono" font-size="10" letter-spacing="1.15" fill="#626761">ONE REMOTE-SUPPORT SESSION</text>
  <text x="1100" y="100" class="mono" font-size="10" text-anchor="end" letter-spacing="1.15" fill="#626761">HOST / LOCAL</text>
  <line x1="754" y1="120" x2="1126" y2="120" stroke="#0D100F"/>

  <text x="782" y="190" class="mono" font-size="10" letter-spacing="1.15" fill="#626761">REMOTE HELPER</text>
  <text x="782" y="218" class="sans" font-size="21" font-weight="700" fill="#0D100F">asks to</text>
  <text x="782" y="242" class="sans" font-size="21" font-weight="700" fill="#0D100F">view</text>
  <text x="782" y="264" class="sans" font-size="12" fill="#626761">link shared privately</text>
  <text x="782" y="297" class="mono" font-size="10" letter-spacing="1.1" fill="#BD4627">REQUEST</text>
  <line x1="846" y1="293" x2="890" y2="293" stroke="#EF5D32"/>
  <path d="M884 287l8 6-8 6" fill="none" stroke="#EF5D32" stroke-width="2"/>

  <rect x="910" y="170" width="166" height="270" fill="#E8E3D9" stroke="#0D100F" stroke-width="7"/>
  <text x="910" y="156" class="mono" font-size="10" letter-spacing="1.1" fill="#0D100F">HOST DEVICE</text>
  <text x="1048" y="392" class="mono" font-size="8" text-anchor="middle" letter-spacing=".8" fill="#626761">VIEW</text>
  <text x="1048" y="405" class="mono" font-size="8" text-anchor="middle" letter-spacing=".8" fill="#626761">ONLY</text>
  <rect x="910" y="170" width="112" height="270" fill="#0D100F"/>
  <rect x="1012" y="170" width="10" height="270" fill="#EF5D32"/>
  <text x="928" y="207" class="mono" font-size="9" letter-spacing="1" fill="#F4F1EA">LOCAL</text>
  <text x="928" y="224" class="mono" font-size="9" letter-spacing="1" fill="#F4F1EA">DECISION</text>
  <rect x="992" y="298" width="15" height="15" fill="#45C486"/>

  <line x1="754" y1="468" x2="1126" y2="468" stroke="#0D100F"/>
  <rect x="780" y="495" width="11" height="11" fill="#45C486"/>
  <text x="802" y="505" class="mono" font-size="10" letter-spacing="1" fill="#2F8B63">APPROVE HERE</text>
  <text x="1100" y="505" class="mono" font-size="10" text-anchor="end" letter-spacing="1" fill="#626761">STOP ANY TIME</text>
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
          background: "#F4F1EA",
          surface: "#FFFDF8",
          text: "#0D100F",
          action: "#EF5D32",
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
