export class ApiError extends Error {
    constructor(message, status, details = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.details = details;
    }
}

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
    element.textContent = message;
    element.dataset.type = type;
    element.hidden = !message;
}

export function setFormBusy(form, busy) {
    form.setAttribute("aria-busy", String(busy));
    form.querySelectorAll("button, input, select").forEach((control) => {
        control.disabled = busy;
    });
}
