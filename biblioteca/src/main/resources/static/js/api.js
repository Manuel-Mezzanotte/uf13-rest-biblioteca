export class ApiError extends Error {
    constructor(message, status, details = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.details = details;
    }
}

const feedbackTimers = new WeakMap();
const buttonStates = new WeakMap();
const controlStates = new WeakMap();

export async function apiRequest(path, options = {}) {
    const response = await fetch(path, {
        cache: "no-store",
        ...options,
        headers: {
            Accept: "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers
        }
    });

    let payload;

    try {
        payload = await response.json();
    } catch {
        throw new ApiError("Il server ha restituito una risposta non valida", response.status);
    }

    if (!response.ok || payload.status !== "success") {
        throw new ApiError(
            payload.message || "Operazione non riuscita",
            response.status,
            payload.data || {}
        );
    }

    return payload.data;
}

export function clearFieldErrors(form) {
    form.querySelectorAll("[data-error-for]").forEach((element) => {
        element.textContent = "";
    });

    form.querySelectorAll("[aria-invalid='true']").forEach((field) => {
        field.removeAttribute("aria-invalid");
    });
}

export function applyFieldErrors(form, details) {
    Object.entries(details).forEach(([fieldName, message]) => {
        const errorElement = form.querySelector(`[data-error-for="${fieldName}"]`);
        const field = form.elements.namedItem(fieldName);

        if (errorElement) {
            errorElement.textContent = message;
        }

        if (field instanceof HTMLElement) {
            field.setAttribute("aria-invalid", "true");
        }
    });
}

export function setFeedback(element, message = "", type = "error") {
    const activeTimer = feedbackTimers.get(element);

    if (activeTimer) {
        window.clearTimeout(activeTimer);
        feedbackTimers.delete(element);
    }

    element.textContent = message;
    element.hidden = !message;

    if (!message) {
        element.removeAttribute("data-type");
        return;
    }

    element.dataset.type = type;

    if (type === "success") {
        const timer = window.setTimeout(() => {
            element.hidden = true;
            element.textContent = "";
            element.removeAttribute("data-type");
            feedbackTimers.delete(element);
        }, 4000);
        feedbackTimers.set(element, timer);
    }
}

export function setButtonBusy(button, busy, busyLabel = "Operazione in corso…") {
    if (busy) {
        if (!buttonStates.has(button)) {
            buttonStates.set(button, {
                label: button.textContent.trim(),
                disabled: button.disabled
            });
        }

        button.textContent = busyLabel;
        button.disabled = true;
        button.classList.add("is-loading");
        button.setAttribute("aria-busy", "true");
        return;
    }

    const previousState = buttonStates.get(button);

    if (!previousState) {
        return;
    }

    button.textContent = previousState.label;
    button.disabled = previousState.disabled;
    button.classList.remove("is-loading");
    button.removeAttribute("aria-busy");
    buttonStates.delete(button);
}

export function setFormBusy(form, busy, busyLabel = "Salvataggio…") {
    form.setAttribute("aria-busy", String(busy));
    const submitButton = form.querySelector("button[type='submit']");
    const controls = [...form.querySelectorAll("input, select, button")]
        .filter((control) => control !== submitButton);

    if (busy) {
        controls.forEach((control) => {
            controlStates.set(control, control.disabled);
            control.disabled = true;
        });
        setButtonBusy(submitButton, true, busyLabel);
        return;
    }

    controls.forEach((control) => {
        if (controlStates.has(control)) {
            control.disabled = controlStates.get(control);
            controlStates.delete(control);
        }
    });
    setButtonBusy(submitButton, false);
}
