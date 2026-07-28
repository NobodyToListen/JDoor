const toggle = document.querySelector("[data-menu-toggle]");
const navigation = document.querySelector("[data-site-nav]");
const header = document.querySelector("[data-header]");

function setMenuOpen(open, { restoreFocus = false } = {}) {
  if (!(toggle instanceof HTMLButtonElement) || !(navigation instanceof HTMLElement)) return;

  toggle.setAttribute("aria-expanded", String(open));
  toggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
  navigation.toggleAttribute("data-open", open);

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
  if (event.target instanceof Element && event.target.closest("a")) {
    setMenuOpen(false);
  }
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && toggle?.getAttribute("aria-expanded") === "true") {
    event.preventDefault();
    setMenuOpen(false, { restoreFocus: true });
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
