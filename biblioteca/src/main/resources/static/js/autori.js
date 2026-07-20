import {
    ApiError,
    apiRequest,
    applyFieldErrors,
    clearFieldErrors,
    setButtonBusy,
    setFeedback,
    setFormBusy
} from "./api.js";
import { confirmDeletion } from "./ui.js";

const form = document.querySelector("#autore-form");
const tableBody = document.querySelector("#autori-table-body");
const tableWrapper = document.querySelector("#autori-table-wrapper");
const emptyState = document.querySelector("#autori-empty");
const loadingState = document.querySelector("#autori-loading");
const countElement = document.querySelector("#autori-count");
const feedback = document.querySelector("#autori-feedback");

let autori = [];

function formatCount(count) {
    return count === 1 ? "1 autore" : `${count} autori`;
}

function createAuthorRow(autore) {
    const row = document.createElement("tr");

    const nameCell = document.createElement("td");
    const name = document.createElement("strong");
    name.className = "primary-cell";
    name.textContent = `${autore.nome} ${autore.cognome}`;
    nameCell.append(name);

    const idCell = document.createElement("td");
    const id = document.createElement("span");
    id.className = "identifier";
    id.textContent = `#${autore.id}`;
    idCell.append(id);

    const actionsCell = document.createElement("td");
    actionsCell.className = "row-actions";
    const deleteButton = document.createElement("button");
    deleteButton.className = "button button--danger button--small";
    deleteButton.type = "button";
    deleteButton.dataset.deleteAuthor = String(autore.id);
    deleteButton.textContent = "Elimina";
    deleteButton.setAttribute("aria-label", `Elimina ${autore.nome} ${autore.cognome}`);
    actionsCell.append(deleteButton);

    row.append(nameCell, idCell, actionsCell);
    return row;
}

function renderAuthors() {
    tableBody.replaceChildren(...autori.map(createAuthorRow));
    countElement.textContent = formatCount(autori.length);
    tableWrapper.hidden = autori.length === 0;
    emptyState.hidden = autori.length > 0;
}

function setListLoading(loading) {
    loadingState.hidden = !loading;

    if (loading) {
        countElement.textContent = "Caricamento…";
        tableWrapper.hidden = true;
        emptyState.hidden = true;
    }
}

async function loadAuthors() {
    setListLoading(true);

    try {
        autori = await apiRequest("/autori");
        autori.sort((first, second) =>
            `${first.cognome} ${first.nome}`.localeCompare(`${second.cognome} ${second.nome}`, "it")
        );
        setListLoading(false);
        renderAuthors();
        document.dispatchEvent(new CustomEvent("biblioteca:autori-updated", {
            detail: { autori }
        }));
    } catch (error) {
        setListLoading(false);
        countElement.textContent = "Dati non disponibili";
        setFeedback(feedback, error.message || "Impossibile caricare gli autori");
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    setFeedback(feedback);
    const formData = new FormData(form);
    setFormBusy(form, true);

    try {
        await apiRequest("/autori/add", {
            method: "POST",
            body: JSON.stringify({
                nome: formData.get("nome")?.trim(),
                cognome: formData.get("cognome")?.trim()
            })
        });
        form.reset();
        setFeedback(feedback, "Autore aggiunto correttamente", "success");
        await loadAuthors();
    } catch (error) {
        if (error instanceof ApiError) {
            applyFieldErrors(form, error.details);
        }
        setFeedback(feedback, error.message || "Impossibile aggiungere l’autore");
    } finally {
        setFormBusy(form, false);
    }
});

tableBody.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-delete-author]");

    if (!button) {
        return;
    }

    const autore = autori.find((item) => String(item.id) === button.dataset.deleteAuthor);
    const authorName = autore ? `${autore.nome} ${autore.cognome}` : "questo autore";
    const confirmed = await confirmDeletion({
        title: "Eliminare l’autore?",
        message: `Eliminando ${authorName} verranno rimossi anche gli eventuali libri associati.`
    });

    if (!confirmed) {
        return;
    }

    setButtonBusy(button, true, "Eliminazione…");
    setFeedback(feedback);

    try {
        await apiRequest(`/autori/${encodeURIComponent(button.dataset.deleteAuthor)}`, {
            method: "DELETE"
        });
        setFeedback(feedback, "Autore eliminato correttamente", "success");
        await loadAuthors();
    } catch (error) {
        setFeedback(feedback, error.message || "Impossibile eliminare l’autore");
        setButtonBusy(button, false);
    }
});

export function initAutori() {
    return loadAuthors();
}
