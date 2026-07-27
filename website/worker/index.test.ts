import assert from "node:assert/strict";
import test from "node:test";
import { handleRequest, hardenResponse } from "./handler.js";

function assetEnv(response: Response): Pick<Env, "ASSETS"> {
  return {
    ASSETS: {
      async fetch() {
        return response.clone();
      },
      connect() {
        throw new Error("Socket access is not used by the website.");
      }
    }
  };
}

test("serves static HTML with a strict, browser-compatible security policy", async () => {
  const request = new Request("https://jdoor.ejupilabs.com/");
  const response = await handleRequest(
    request,
    assetEnv(new Response("<!doctype html><title>JDoor Assist</title>", {
      headers: { "Content-Type": "text/html; charset=utf-8" }
    }))
  );

  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-Frame-Options"), "DENY");
  assert.equal(response.headers.get("X-Content-Type-Options"), "nosniff");
  assert.equal(response.headers.get("Cross-Origin-Opener-Policy"), "same-origin");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=0, must-revalidate");
  assert.match(response.headers.get("Content-Security-Policy") ?? "", /connect-src 'none'/);
  assert.match(response.headers.get("Permissions-Policy") ?? "", /camera=\(\)/);
});

test("keeps the custom 404 unindexable and uncached", () => {
  const response = hardenResponse(
    new Request("https://jdoor.ejupilabs.com/missing"),
    new Response("Missing", { status: 404, headers: { "Content-Type": "text/html" } })
  );

  assert.equal(response.status, 404);
  assert.equal(response.headers.get("X-Robots-Tag"), "noindex, follow");
  assert.equal(response.headers.get("Cache-Control"), "no-store");
});

test("rejects mutating HTTP methods before the asset binding", async () => {
  const response = await handleRequest(
    new Request("https://jdoor.ejupilabs.com/", { method: "POST" }),
    assetEnv(new Response("should not be served"))
  );

  assert.equal(response.status, 405);
  assert.equal(response.headers.get("Allow"), "GET, HEAD");
  assert.equal(await response.text(), "Method not allowed");
});

test("HEAD responses preserve metadata without a body", async () => {
  const response = await handleRequest(
    new Request("https://jdoor.ejupilabs.com/styles.css", { method: "HEAD" }),
    assetEnv(new Response("body", { headers: { "Content-Type": "text/css" } }))
  );

  assert.equal(await response.text(), "");
  assert.equal(response.headers.get("Cache-Control"), "public, max-age=3600, must-revalidate");
});
