# Relazione tecnica - Esame UF13

## Progetto di partenza

Il progetto è un fork della repository fornita dal docente ed è stato sviluppato a partire dal branch `lecture/lz-1`. Le modifiche della prima task sono state realizzate nel branch `feature/task-1`.

## Task 1 - Risposte standard e gestione degli errori

### Obiettivo

Lo scopo della task era rendere uniformi le risposte delle API e spostare la gestione degli errori fuori dai controller. Prima delle modifiche alcuni endpoint restituivano DTO, altri liste o semplici stringhe.

### Scelte progettuali

Per uniformare le risposte ho usato la classe generica `APIResponse<T>` come formato comune. La risposta può contenere:

- `status`, con i valori `success`, `fail` oppure `error`;
- `data`, con il risultato dell'operazione;
- `message` e `code` per descrivere un errore;
- `results` quando viene restituita una collezione.

Gli stati dell'enum `APIResponseStatus` vengono serializzati in minuscolo tramite `@JsonValue`.

I controller di autori e libri restituiscono sempre oggetti `APIResponse<T>` contenenti DTO. Per le risorse mancanti ho usato `ResponseStatusException`, evitando blocchi `try-catch` nei controller.

Ho concentrato la gestione delle eccezioni nel `GlobalExceptionHandler`:

- gli errori di validazione producono una risposta `400 fail` con una mappa campo-messaggio;
- le risorse non trovate producono una risposta `404 fail`;
- gli errori imprevisti producono una risposta `500 error` con un messaggio generico.

Gli stack trace completi vengono scritti nei log, ma non sono inclusi nelle risposte inviate al client.

### Verifiche eseguite

Per la verifica ho avviato l'applicazione con Java 21, profilo `dev` e database MySQL. Ho poi provato gli endpoint tramite richieste HTTP reali.

Sono stati verificati:

- inserimento, lettura e cancellazione di autori e libri;
- azzeramento dell'ID ricevuto durante la creazione di un autore;
- validazione dei campi obbligatori con risposta `400`;
- ricerca e cancellazione di risorse inesistenti con risposta `404`;
- creazione di un libro associato a un autore inesistente;
- risposta `500` ottenuta interrompendo temporaneamente il database;
- assenza di stack trace e dettagli JDBC nel JSON restituito al client.

È stato inoltre eseguito il test Maven presente nel progetto:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Criteri di accettazione soddisfatti

- Tutti gli endpoint utilizzano lo stesso formato di risposta.
- Le entità JPA non vengono esposte dai controller.
- La validazione restituisce una mappa degli errori associata ai singoli campi.
- La gestione degli errori è centralizzata con `@ControllerAdvice`.
- Nei controller non sono presenti blocchi `try-catch` usati per formattare gli errori HTTP.
- Gli errori interni non espongono stack trace al client.
- I tipi generici usati dall'handler sono espliciti, ad esempio `Void` e `Map<String, String>`.
