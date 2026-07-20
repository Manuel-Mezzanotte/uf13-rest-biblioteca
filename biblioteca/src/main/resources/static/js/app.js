import { initAutori } from "./autori.js";
import { initLibri } from "./libri.js";

const tabs = [...document.querySelectorAll("[data-view]")];
const panels = [...document.querySelectorAll("[data-view-panel]")];
const apiStatus = document.querySelector("#api-status");
const apiStatusText = document.querySelector("#api-status-text");

const availableViews = new Set(tabs.map((tab) => tab.dataset.view));

function activateView(viewName, updateHash = true) {
    const nextView = availableViews.has(viewName) ? viewName : "libri";

    tabs.forEach((tab) => {
        const isActive = tab.dataset.view === nextView;
        tab.classList.toggle("is-active", isActive);
        tab.setAttribute("aria-selected", String(isActive));
        tab.tabIndex = isActive ? 0 : -1;
    });

    panels.forEach((panel) => {
        panel.hidden = panel.dataset.viewPanel !== nextView;
    });

    if (updateHash && window.location.hash !== `#${nextView}`) {
        history.replaceState(null, "", `#${nextView}`);
    }
}

tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => activateView(tab.dataset.view));

    tab.addEventListener("keydown", (event) => {
        if (!["ArrowLeft", "ArrowRight"].includes(event.key)) {
            return;
        }

        event.preventDefault();
        const direction = event.key === "ArrowRight" ? 1 : -1;
        const nextIndex = (index + direction + tabs.length) % tabs.length;
        tabs[nextIndex].focus();
        activateView(tabs[nextIndex].dataset.view);
    });
});

window.addEventListener("hashchange", () => {
    activateView(window.location.hash.slice(1), false);
});

async function checkApiHealth() {
    try {
        const response = await fetch("/actuator/health", {
            headers: { Accept: "application/json" },
            cache: "no-store"
        });
        const health = await response.json();

        if (!response.ok || health.status !== "UP") {
            throw new Error("API not available");
        }

        apiStatus.dataset.state = "online";
        apiStatusText.textContent = "API disponibile";
    } catch {
        apiStatus.dataset.state = "offline";
        apiStatusText.textContent = "API non disponibile";
    }
}

activateView(window.location.hash.slice(1), false);
checkApiHealth();
initAutori();
initLibri();
