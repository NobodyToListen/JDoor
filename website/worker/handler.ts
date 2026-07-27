const SECURITY_HEADERS = Object.freeze({
  "Content-Security-Policy":
    "default-src 'self'; base-uri 'none'; connect-src 'none'; font-src 'self'; form-action 'none'; frame-ancestors 'none'; img-src 'self' data:; object-src 'none'; script-src 'self'; style-src 'self'; upgrade-insecure-requests",
  "Cross-Origin-Opener-Policy": "same-origin",
  "Cross-Origin-Resource-Policy": "same-origin",
  "Permissions-Policy":
    "camera=(), microphone=(), geolocation=(), payment=(), usb=(), browsing-topics=(), screen-wake-lock=()",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "Strict-Transport-Security": "max-age=63072000; includeSubDomains; preload",
  "X-Content-Type-Options": "nosniff",
  "X-Frame-Options": "DENY"
});

const METHOD_NOT_ALLOWED = new Response("Method not allowed", {
  status: 405,
  headers: {
    "Allow": "GET, HEAD",
    "Content-Type": "text/plain; charset=utf-8"
  }
});

function cachePolicy(request: Request, response: Response): string {
  if (response.status >= 400) return "no-store";

  const contentType = response.headers.get("Content-Type")?.toLowerCase() ?? "";
  if (contentType.includes("text/html")) return "public, max-age=0, must-revalidate";

  const pathname = new URL(request.url).pathname;
  if (/\.(?:avif|ico|jpe?g|png|svg|webp|woff2)$/i.test(pathname)) {
    return "public, max-age=604800, stale-while-revalidate=86400";
  }

  return "public, max-age=3600, must-revalidate";
}

export function hardenResponse(request: Request, response: Response): Response {
  const headers = new Headers(response.headers);
  for (const [name, value] of Object.entries(SECURITY_HEADERS)) {
    headers.set(name, value);
  }
  headers.set("Cache-Control", cachePolicy(request, response));

  if (response.status === 404) {
    headers.set("X-Robots-Tag", "noindex, follow");
  }

  return new Response(request.method === "HEAD" ? null : response.body, {
    status: response.status,
    statusText: response.statusText,
    headers
  });
}

export async function handleRequest(request: Request, env: Pick<Env, "ASSETS">): Promise<Response> {
  if (request.method !== "GET" && request.method !== "HEAD") {
    return hardenResponse(request, METHOD_NOT_ALLOWED.clone());
  }

  const assetResponse = await env.ASSETS.fetch(request);
  return hardenResponse(request, assetResponse);
}
