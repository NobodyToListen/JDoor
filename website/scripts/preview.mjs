import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { extname, resolve, sep } from "node:path";

const PUBLIC_DIRECTORY = resolve(import.meta.dirname, "..", "public");
const portArgument = process.argv.indexOf("--port");
const PORT = portArgument >= 0 ? Number(process.argv[portArgument + 1]) : 4175;
const MIME_TYPES = new Map([
  [".avif", "image/avif"],
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".png", "image/png"],
  [".svg", "image/svg+xml; charset=utf-8"],
  [".txt", "text/plain; charset=utf-8"],
  [".webp", "image/webp"],
  [".webmanifest", "application/manifest+json; charset=utf-8"],
  [".xml", "application/xml; charset=utf-8"]
]);

function safePath(pathname) {
  const relative = pathname === "/" ? "index.html" : decodeURIComponent(pathname).replace(/^\/+/, "");
  const candidate = resolve(PUBLIC_DIRECTORY, relative);
  return candidate === PUBLIC_DIRECTORY || candidate.startsWith(`${PUBLIC_DIRECTORY}${sep}`)
    ? candidate
    : null;
}

async function responseFor(pathname) {
  const candidate = safePath(pathname);
  if (!candidate) return { status: 400, path: null };

  try {
    const file = (await stat(candidate)).isDirectory() ? resolve(candidate, "index.html") : candidate;
    await stat(file);
    return { status: 200, path: file };
  } catch {
    return { status: 404, path: resolve(PUBLIC_DIRECTORY, "404.html") };
  }
}

const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url ?? "/", `http://${request.headers.host ?? "127.0.0.1"}`);
    const result = await responseFor(url.pathname);
    if (!result.path) {
      response.writeHead(result.status, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Bad request");
      return;
    }

    const body = await readFile(result.path);
    response.writeHead(result.status, {
      "Cache-Control": "no-store",
      "Content-Type": MIME_TYPES.get(extname(result.path)) ?? "application/octet-stream"
    });
    response.end(body);
  } catch (error) {
    console.error(error);
    response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Internal preview error");
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`JDoor website preview listening on http://127.0.0.1:${PORT}`);
});
