const confirmationDialog = document.querySelector("#confirmation-dialog");
const confirmationTitle = document.querySelector("#confirmation-title");
const confirmationMessage = document.querySelector("#confirmation-message");
const confirmationSubmit = document.querySelector("#confirmation-submit");
const confirmationCancel = document.querySelector("#confirmation-cancel");

confirmationDialog.addEventListener("click", (event) => {
    if (event.target === confirmationDialog) {
        confirmationDialog.close("cancel");
    }
});

export function confirmDeletion({
    title = "Conferma eliminazione",
    message,
    confirmLabel = "Elimina"
}) {
    confirmationTitle.textContent = title;
    confirmationMessage.textContent = message;
    confirmationSubmit.textContent = confirmLabel;
    confirmationDialog.returnValue = "cancel";

    return new Promise((resolve) => {
        confirmationDialog.addEventListener("close", () => {
            resolve(confirmationDialog.returnValue === "confirm");
        }, { once: true });

        confirmationDialog.showModal();
        confirmationCancel.focus();
    });
}
