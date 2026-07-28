const toggle = document.querySelector("[data-menu-toggle]");
const navigation = document.querySelector("[data-site-nav]");
const header = document.querySelector("[data-header]");
const backgroundTargets = [
  document.querySelector(".skip-link"),
  document.querySelector("#main-content"),
  document.querySelector(".site-footer")
].filter((target) => target instanceof HTMLElement);

function setMenuOpen(open, { restoreFocus = false } = {}) {
  if (!(toggle instanceof HTMLButtonElement) || !(navigation instanceof HTMLElement)) return;

  toggle.setAttribute("aria-expanded", String(open));
  toggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
  navigation.toggleAttribute("data-open", open);
  document.documentElement.toggleAttribute("data-menu-open", open);
  for (const target of backgroundTargets) {
    target.inert = open;
  }

  if (open) {
    setTimeout(() => navigation.querySelector("a")?.focus(), 0);
  } else if (restoreFocus) {
    toggle.focus();
  }
}

toggle?.addEventListener("click", () => {
  setMenuOpen(toggle.getAttribute("aria-expanded") !== "true");
});

navigation?.addEventListener("click", (event) => {
  const link = event.target instanceof Element ? event.target.closest("a") : null;
  if (!(link instanceof HTMLAnchorElement)) return;

  const href = link.getAttribute("href");
  if (!href?.startsWith("#")) {
    setMenuOpen(false);
    return;
  }

  const section = document.getElementById(href.slice(1));
  const headingId = section?.getAttribute("aria-labelledby");
  const heading = headingId ? document.getElementById(headingId) : null;
  if (!(section instanceof HTMLElement) || !(heading instanceof HTMLElement)) {
    setMenuOpen(false);
    return;
  }

  event.preventDefault();
  window.history.pushState(null, "", href);
  setMenuOpen(false);
  section.scrollIntoView({ block: "start" });
  const temporaryTabIndex = !heading.hasAttribute("tabindex");
  if (temporaryTabIndex) heading.setAttribute("tabindex", "-1");
  heading.focus({ preventScroll: true });
  if (temporaryTabIndex) {
    heading.addEventListener("blur", () => heading.removeAttribute("tabindex"), { once: true });
  }
});

document.addEventListener("keydown", (event) => {
  if (toggle?.getAttribute("aria-expanded") !== "true") return;

  if (event.key === "Escape") {
    event.preventDefault();
    setMenuOpen(false, { restoreFocus: true });
    return;
  }

  if (event.key === "Tab" && navigation instanceof HTMLElement && toggle instanceof HTMLButtonElement) {
    const links = [...navigation.querySelectorAll("a[href]")];
    const firstLink = links[0];
    if (!(firstLink instanceof HTMLAnchorElement)) return;

    if (event.shiftKey && document.activeElement === firstLink) {
      event.preventDefault();
      toggle.focus();
    } else if (!event.shiftKey && document.activeElement === toggle) {
      event.preventDefault();
      firstLink.focus();
    }
  }
});

document.addEventListener("click", (event) => {
  if (
    toggle?.getAttribute("aria-expanded") === "true" &&
    event.target instanceof Node &&
    !header?.contains(event.target)
  ) {
    setMenuOpen(false);
  }
});

const desktop = window.matchMedia("(min-width: 1121px)");
desktop.addEventListener("change", (event) => {
  if (event.matches) setMenuOpen(false);
});

const sectionLinks = [...document.querySelectorAll("[data-site-nav] a[href^='#']")];
const sectionById = new Map(
  sectionLinks.map((link) => [link.getAttribute("href")?.slice(1), link])
);

if ("IntersectionObserver" in window) {
  const sectionObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
      if (!visible) return;

      for (const link of sectionLinks) {
        link.removeAttribute("aria-current");
      }
      sectionById.get(visible.target.id)?.setAttribute("aria-current", "location");
    },
    { rootMargin: "-18% 0px -64% 0px", threshold: [0, 0.2, 0.5] }
  );

  for (const id of sectionById.keys()) {
    const section = document.getElementById(id);
    if (section) sectionObserver.observe(section);
  }
}
