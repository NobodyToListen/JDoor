import { expect, test } from "@playwright/test";

test("homepage presents the consent model without a fake web session", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator("h1")).toContainText("stays in charge");
  await expect(page.locator(".status-label")).toContainText(/source 1\.0\.0/i);
  await expect(page.locator("#flow .flow-step")).toHaveCount(5);
  await expect(page.getByRole("link", { name: /Read the source/i })).toHaveAttribute(
    "href",
    "https://github.com/NobodyToListen/JDoor"
  );
  await expect(page.getByRole("button", { name: /start|join|connect/i })).toHaveCount(0);
  await expect(page.locator("form")).toHaveCount(0);
});

test("the origin and trust boundary are stated without overstating identity checks", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator("#story")).toContainText(/2022[\s\S]*school networking project/i);
  await expect(page.locator("#trust h2")).toContainText(/software checks the endpoint/i);
  await expect(page.locator("body")).not.toContainText("Every viewer is verified");
});

test("design decisions expose alternatives and accepted costs", async ({ page }) => {
  await page.goto("/#decisions");

  await expect(page.locator("#decisions article")).toHaveCount(4);
  await expect(page.locator("#decisions dt", { hasText: "Instead of" })).toHaveCount(4);
  await expect(page.locator("#decisions dt", { hasText: "Accepted cost" })).toHaveCount(4);
  await expect(page.locator("#decisions")).toContainText("Java 21 + Swing");
  await expect(page.locator("#decisions")).toContainText("Direct trusted-LAN connection");
});

test("fit guidance names real situations and when to choose another product", async ({ page }) => {
  await page.goto("/#boundaries");

  await expect(page.locator("#boundaries")).toContainText("Helping family");
  await expect(page.locator("#boundaries")).toContainText("classroom or lab");
  await expect(page.locator("#boundaries")).toContainText("small office");
  await expect(page.locator("#boundaries")).toContainText("Choose something else");
});

test("authentic product surfaces load with useful context", async ({ page }) => {
  await page.goto("/#interface");

  const images = page.locator("#interface img");
  await expect(images).toHaveCount(2);
  await expect(images.nth(0)).toHaveAttribute("alt", /launcher on Windows/i);
  await expect(images.nth(1)).toHaveAttribute("alt", /local approval dialog/i);
  await expect
    .poll(() => images.evaluateAll((items) => items.every((image) => image.complete && image.naturalWidth > 0)))
    .toBe(true);
  await expect(page.locator("#interface figcaption")).toContainText([
    "Two roles, one visible boundary",
    "The endpoint is redacted here"
  ]);
});

test("source and evidence links have useful accessible names", async ({ page }) => {
  await page.goto("/#status");

  for (const name of ["Architecture", "Threat model", "Privacy", "Security policy"]) {
    await expect(page.getByRole("link", { name: new RegExp(`${name}.*opens in a new tab`, "i") })).toHaveCount(1);
  }
});

test("mobile navigation opens, focuses its first item and closes with Escape", async ({ page, isMobile }) => {
  test.skip(!isMobile, "Mobile navigation contract");
  await page.goto("/");

  const toggle = page.locator("[data-menu-toggle]");
  const navigation = page.locator("[data-site-nav]");
  await expect(toggle).toHaveAttribute("aria-controls", "site-navigation");
  await expect(navigation).toHaveAttribute("id", "site-navigation");
  await toggle.click();
  await expect(toggle).toHaveAttribute("aria-expanded", "true");
  await expect(navigation).toHaveAttribute("data-open", "");
  await expect(navigation.getByRole("link").first()).toBeFocused();
  await expect(page.locator("#main-content")).toHaveJSProperty("inert", true);
  await expect(page.locator("html")).toHaveAttribute("data-menu-open", "");

  await page.keyboard.press("Shift+Tab");
  await expect(toggle).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(navigation.getByRole("link").first()).toBeFocused();

  await page.keyboard.press("Escape");
  await expect(toggle).toHaveAttribute("aria-expanded", "false");
  await expect(navigation).not.toHaveAttribute("data-open", "");
  await expect(toggle).toBeFocused();
  await expect(page.locator("#main-content")).toHaveJSProperty("inert", false);
  await expect(page.locator("html")).not.toHaveAttribute("data-menu-open", "");
});

test("desktop navigation state resets at the same breakpoint used by the layout", async ({ page, isMobile }) => {
  test.skip(isMobile, "Resizable desktop context");
  await page.setViewportSize({ width: 1000, height: 800 });
  await page.goto("/");

  const toggle = page.locator("[data-menu-toggle]");
  const navigation = page.locator("[data-site-nav]");
  await toggle.click();
  await expect(toggle).toHaveAttribute("aria-expanded", "true");
  await expect(navigation).toHaveAttribute("data-open", "");

  await page.setViewportSize({ width: 1200, height: 800 });
  await expect(toggle).toHaveAttribute("aria-expanded", "false");
  await expect(navigation).not.toHaveAttribute("data-open", "");
});

test("section navigation transfers focus to the destination heading", async ({ page }) => {
  await page.goto("/");

  const link = page.locator('[data-site-nav] a[href="#decisions"]');
  if (await page.locator("[data-menu-toggle]").isVisible()) {
    await page.locator("[data-menu-toggle]").click();
  }
  await link.click();
  await expect(page.locator("#decisions-title")).toBeFocused();
  await expect(page).toHaveURL(/#decisions$/);
});

test("the complete hero fits a common laptop viewport", async ({ page, isMobile }) => {
  test.skip(isMobile, "Desktop composition");
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.goto("/");

  const bounds = await page.locator(".hero").evaluate(() => {
    const actions = document.querySelector(".hero__actions")?.getBoundingClientRect();
    const study = document.querySelector(".door-study")?.getBoundingClientRect();
    return {
      actionsBottom: actions?.bottom ?? Number.POSITIVE_INFINITY,
      studyBottom: study?.bottom ?? Number.POSITIVE_INFINITY
    };
  });
  expect(bounds.actionsBottom).toBeLessThanOrEqual(720);
  expect(bounds.studyBottom).toBeLessThanOrEqual(720);
});

test("all key viewport widths avoid horizontal overflow", async ({ page }) => {
  for (const width of [320, 390, 768, 1280, 1920]) {
    await page.setViewportSize({ width, height: width < 800 ? 860 : 1000 });
    await page.goto("/");
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth
    );
    expect(overflow, `overflow at ${width}px`).toBeLessThanOrEqual(1);
  }
});

test("custom missing routes return a true, noindex 404 page", async ({ page }) => {
  const response = await page.goto("/not-a-real-route");
  expect(response?.status()).toBe(404);
  await expect(page.locator("h1")).toContainText("Nothing is listening here");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex, follow");
});

test("reduced motion keeps every section immediately readable", async ({ browser }) => {
  const context = await browser.newContext({ reducedMotion: "reduce" });
  const page = await context.newPage();
  await page.goto("/");
  await expect(page.locator("main")).toBeVisible();
  await expect(page.locator("#status")).toBeVisible();
  await context.close();
});
