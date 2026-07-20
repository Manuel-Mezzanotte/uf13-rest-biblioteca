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

const form = document.querySelector("#libro-form");
const authorSelect = document.querySelector("#libro-autore");
const authorNote = document.querySelector("#libro-autore-note");
const submitButton = document.querySelector("#libro-submit");
const searchForm = document.querySelector("#libri-search-form");
const searchType = document.querySelector("#libri-search-type");
const searchQuery = document.querySelector("#libri-search-query");
const searchReset = document.querySelector("#libri-search-reset");
const tableBody = document.querySelector("#libri-table-body");
const tableWrapper = document.querySelector("#libri-table-wrapper");
const emptyState = document.querySelector("#libri-empty");
const loadingState = document.querySelector("#libri-loading");
const emptyTitle = document.querySelector("#libri-empty-title");
const emptyCopy = document.querySelector("#libri-empty-copy");
const countElement = document.querySelector("#libri-count");
const feedback = document.querySelector("#libri-feedback");

let libri = [];
let autori = [];
let searchActive = false;

function formatCount(count) {
    return count === 1 ? "1 libro" : `${count} libri`;
}

function getAuthorName(authorId) {
    const autore = autori.find((item) => item.id === authorId);
    return autore ? `${autore.nome} ${autore.cognome}` : `Autore #${authorId}`;
}

function createBookRow(libro) {
    const row = document.createElement("tr");

    const titleCell = document.createElement("td");
    const title = document.createElement("strong");
    title.className = "primary-cell";
    title.textContent = libro.titolo;
    const mobileAuthor = document.createElement("span");
    mobileAuthor.className = "mobile-book-meta";
    mobileAuthor.textContent = getAuthorName(libro.autore);
    titleCell.append(title, mobileAuthor);

    const isbnCell = document.createElement("td");
    const isbn = document.createElement("span");
    isbn.className = "identifier identifier--isbn";
    isbn.textContent = libro.isbn;
    isbnCell.append(isbn);

    const detailsCell = document.createElement("td");
    detailsCell.className = "optional-column secondary-cell";
    detailsCell.textContent = [libro.genere, libro.anno].filter(Boolean).join(" · ") || "—";

    const authorCell = document.createElement("td");
    authorCell.className = "author-column secondary-cell";
    authorCell.textContent = getAuthorName(libro.autore);

    const actionsCell = document.createElement("td");
    actionsCell.className = "row-actions";
    const deleteButton = document.createElement("button");
    deleteButton.className = "button button--danger button--small";
    deleteButton.type = "button";
    deleteButton.dataset.deleteBook = libro.isbn;
    deleteButton.textContent = "Elimina";
    deleteButton.setAttribute("aria-label", `Elimina ${libro.titolo}`);
    actionsCell.append(deleteButton);

    row.append(titleCell, isbnCell, detailsCell, authorCell, actionsCell);
    return row;
}

function renderBooks() {
    tableBody.replaceChildren(...libri.map(createBookRow));
    countElement.textContent = searchActive
        ? (libri.length === 1 ? "1 libro trovato" : `${libri.length} libri trovati`)
        : formatCount(libri.length);
    tableWrapper.hidden = libri.length === 0;
    emptyState.hidden = libri.length > 0;
    searchReset.hidden = !searchActive;

    emptyTitle.textContent = searchActive ? "Nessun risultato" : "Nessun libro presente";
    emptyCopy.textContent = searchActive
        ? "Modifica la ricerca oppure torna all’elenco completo."
        : "Aggiungi il primo libro usando il modulo.";
}

function setListLoading(loading, label = "Caricamento…") {
    loadingState.hidden = !loading;

    if (loading) {
        loadingState.querySelector("span:last-child").textContent = label;
        countElement.textContent = label;
        tableWrapper.hidden = true;
        emptyState.hidden = true;
    }
}

function renderAuthorOptions() {
    const currentValue = authorSelect.value;
    authorSelect.replaceChildren();

    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = autori.length > 0 ? "Seleziona un autore" : "Nessun autore disponibile";
    authorSelect.append(placeholder);

    autori.forEach((autore) => {
        const option = document.createElement("option");
        option.value = String(autore.id);
        option.textContent = `${autore.nome} ${autore.cognome}`;
        authorSelect.append(option);
    });

    if (autori.some((autore) => String(autore.id) === currentValue)) {
        authorSelect.value = currentValue;
    }

    const hasAuthors = autori.length > 0;
    authorSelect.disabled = !hasAuthors;
    submitButton.disabled = !hasAuthors;
    authorNote.hidden = hasAuthors;
}

async function loadAuthors() {
    autori = await apiRequest("/autori");
    autori.sort((first, second) =>
        `${first.cognome} ${first.nome}`.localeCompare(`${second.cognome} ${second.nome}`, "it")
    );
    renderAuthorOptions();
}

async function loadBooks() {
    searchActive = false;
    setListLoading(true, "Caricamento catalogo…");

    try {
        libri = await apiRequest("/libri");
        libri.sort((first, second) => first.titolo.localeCompare(second.titolo, "it"));
        setListLoading(false);
        renderBooks();
    } catch (error) {
        setListLoading(false);
        countElement.textContent = "Dati non disponibili";
        setFeedback(feedback, error.message || "Impossibile caricare i libri");
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFieldErrors(form);
    setFeedback(feedback);
    const formData = new FormData(form);
    const yearValue = formData.get("anno")?.trim();
    setFormBusy(form, true);

    try {
        await apiRequest("/libri/add", {
            method: "POST",
            body: JSON.stringify({
                isbn: formData.get("isbn")?.trim(),
                titolo: formData.get("titolo")?.trim(),
                genere: formData.get("genere")?.trim() || null,
                anno: yearValue ? Number(yearValue) : null,
                autore: Number(formData.get("autore"))
            })
        });
        form.reset();
        renderAuthorOptions();
        setFeedback(feedback, "Libro aggiunto correttamente", "success");
        await loadBooks();
    } catch (error) {
        if (error instanceof ApiError) {
            applyFieldErrors(form, error.details);
        }
        setFeedback(feedback, error.message || "Impossibile aggiungere il libro");
    } finally {
        setFormBusy(form, false);
        renderAuthorOptions();
    }
});

searchForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const query = searchQuery.value.trim();

    if (!query) {
        searchQuery.focus();
        return;
    }

    setFeedback(feedback);
    searchActive = true;
    setListLoading(true, "Ricerca in corso…");
    setFormBusy(searchForm, true, "Ricerca…");

    const path = searchType.value === "isbn"
        ? `/libri/${encodeURIComponent(query)}`
        : `/libri/libro?titolo=${encodeURIComponent(query)}`;

    try {
        const libro = await apiRequest(path);
        libri = [libro];
    } catch (error) {
        libri = [];
        if (!(error instanceof ApiError && error.status === 404)) {
            setFeedback(feedback, error.message || "Impossibile completare la ricerca");
        }
    } finally {
        setListLoading(false);
        setFormBusy(searchForm, false);
    }

    renderBooks();
});

searchReset.addEventListener("click", async () => {
    searchForm.reset();
    searchQuery.placeholder = "Titolo esatto";
    setFeedback(feedback);
    await loadBooks();
});

searchType.addEventListener("change", () => {
    searchQuery.placeholder = searchType.value === "isbn" ? "ISBN esatto" : "Titolo esatto";
});

tableBody.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-delete-book]");

    if (!button) {
        return;
    }

    const libro = libri.find((item) => item.isbn === button.dataset.deleteBook);
    const bookTitle = libro?.titolo || "questo libro";
    const confirmed = await confirmDeletion({
        title: "Eliminare il libro?",
        message: `“${bookTitle}” verrà rimosso definitivamente dal catalogo.`
    });

    if (!confirmed) {
        return;
    }

    setButtonBusy(button, true, "Eliminazione…");
    setFeedback(feedback);

    try {
        await apiRequest(`/libri/${encodeURIComponent(button.dataset.deleteBook)}`, {
            method: "DELETE"
        });
        setFeedback(feedback, "Libro eliminato correttamente", "success");
        await loadBooks();
    } catch (error) {
        setFeedback(feedback, error.message || "Impossibile eliminare il libro");
        setButtonBusy(button, false);
    }
});

document.addEventListener("biblioteca:autori-updated", async (event) => {
    autori = event.detail.autori;
    renderAuthorOptions();
    await loadBooks();
});

export async function initLibri() {
    try {
        await loadAuthors();
        await loadBooks();
    } catch (error) {
        setListLoading(false);
        countElement.textContent = "Dati non disponibili";
        setFeedback(feedback, error.message || "Impossibile inizializzare il catalogo");
    }
}
