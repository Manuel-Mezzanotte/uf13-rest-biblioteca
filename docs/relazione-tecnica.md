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

## Task 2 - Containerizzazione e profili

### Obiettivo

Lo scopo della task era rendere la compilazione indipendente dalla macchina dello sviluppatore e separare il comportamento dell'applicazione tra sviluppo e produzione.

### Scelte progettuali

Ho organizzato il `Dockerfile` in due stadi. Il primo usa Maven e JDK 21 per compilare il progetto, mentre il secondo usa solamente una JRE 21 minimale e contiene il file `biblioteca.jar`. Il profilo predefinito dell'immagine è `prod`, ma può essere sostituito tramite una variabile d'ambiente.

Nel file `docker-compose.yaml` ho configurato il backend insieme a MySQL. I servizi condividono una rete dedicata e vengono controllati tramite healthcheck. I dati del database sono conservati in un volume Docker, mentre la cartella `logs` del backend è collegata all'host.

Per il logging ho usato i profili `dev` e `prod`:

- in sviluppo i log vengono mostrati in console con livello `TRACE`, comprese le query SQL di Hibernate;
- in produzione il livello principale è `INFO` e i log vengono scritti nel file `logs/biblioteca.log`;
- i file di produzione ruotano in base alla data e alla dimensione, con un limite di 10 MB per file, 30 giorni di storico e 1 GB complessivo.

### Verifiche eseguite

Ho eseguito una build Docker senza cache per controllare che la compilazione avvenisse interamente nel container. Nell'immagine finale sono presenti la JRE e il JAR dell'applicazione, mentre Maven, il compilatore Java e i sorgenti non sono inclusi.

Con Docker Compose ho verificato l'avvio di MySQL e del backend, lo stato `healthy` di entrambi i servizi e la risposta `UP` dell'endpoint `/actuator/health`. L'endpoint `/autori` ha restituito correttamente la risposta standard dell'API.

In modalità `prod` i log sono stati scritti nel file montato sull'host senza messaggi `TRACE` o `DEBUG` in console. In modalità `dev` sono stati invece verificati i messaggi `TRACE` e la query SQL generata da Hibernate.

È stato infine eseguito il test Maven presente nel progetto con Java 21:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Criteri di accettazione soddisfatti

- La compilazione viene eseguita nello stadio Docker di build.
- L'immagine runtime contiene solamente ciò che serve per avviare l'applicazione.
- I profili `dev` e `prod` applicano livelli e destinazioni di logging differenti.
- Le query SQL sono tracciate durante lo sviluppo.
- I log di produzione sono salvati sull'host e ruotano per tempo e dimensione.
- Backend e database possono essere avviati insieme tramite Docker Compose.

## Task 3 - Monitoraggio e alerting

### Obiettivo

Lo scopo della task era rendere visibili lo stato e le principali metriche dell'applicazione, aggiungendo anche un allarme automatico in caso di troppi errori HTTP 500.

### Scelte progettuali

Ho integrato Spring Boot Actuator e Micrometer per esporre le metriche nel formato Prometheus. Nel file `docker-compose.yaml` sono stati aggiunti Prometheus e Grafana, entrambi con healthcheck e volume persistente. Prometheus interroga l'endpoint `/actuator/prometheus` del backend ogni 15 secondi.

Grafana viene configurato automaticamente tramite file di provisioning. Il datasource Prometheus e la dashboard `Biblioteca - JVM` sono quindi disponibili senza configurazioni manuali. La dashboard mostra stato del servizio, memoria heap, CPU, thread JVM, richieste HTTP e latenza media.

La regola `Troppi errori HTTP 500` usa la seguente query PromQL:

```promql
sum(increase(http_server_requests_seconds_count{job="biblioteca", status="500"}[1m]))
```

La regola viene valutata ogni 10 secondi e passa allo stato `Firing` quando, nell'ultimo minuto, vengono registrati più di 10 errori. È inoltre collegata al pannello della dashboard dedicato agli stati HTTP.

### Verifiche eseguite

Con Docker Compose ho verificato lo stato `healthy` di MySQL, backend, Prometheus e Grafana. L'endpoint Actuator ha restituito `UP`, il target del backend è risultato attivo in Prometheus e il datasource Grafana ha risposto correttamente.

Sono state controllate tutte le query della dashboard e i nove pannelli hanno mostrato dati senza errori o valori mancanti. La prova dell'alert è stata eseguita creando prima un campione HTTP 500, attendendo lo scrape di Prometheus e generando poi 12 nuove risposte 500. Il valore calcolato ha superato la soglia, la regola è passata da `Normal` a `Firing` ed è tornata automaticamente a `Normal` al termine della finestra di un minuto.

È stato infine eseguito il test Maven con Java 21, profilo `prod` e database MySQL attivo:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Criteri di accettazione soddisfatti

- Le metriche del backend sono esposte tramite Actuator e raccolte da Prometheus.
- Grafana e Prometheus vengono avviati insieme all'applicazione tramite Docker Compose.
- La dashboard JVM è configurata automaticamente ed è accessibile dal browser.
- L'alert controlla l'incremento degli HTTP 500 e usa la soglia richiesta di 10 errori.
- Il passaggio visivo tra gli stati `Normal` e `Firing` è stato verificato tramite una simulazione reale.

## Task 4 - Robustezza del codice e automazione dei test

### Obiettivo

Lo scopo della task era verificare in modo automatico il comportamento dei controller e dei service, controllando sia i casi di successo sia i principali flussi di errore.

### Scelte progettuali

Per i controller ho usato `@WebMvcTest` insieme a `MockMvc`, sostituendo i service con mock. Le risposte sono state controllate tramite `jsonPath`, verificando i campi `status`, `data`, `message` e `code` nei casi `200`, `400` e `404`.

I service sono stati testati senza caricare Spring, usando `@ExtendWith(MockitoExtension.class)`, `@Mock` e `@InjectMocks`. Con `verify()` sono state controllate le chiamate a repository e mapper. Nel test di salvataggio dell'autore, `argThat()` verifica che un ID ricevuto dal client venga impostato a `null` prima del salvataggio.

### Verifiche eseguite

I test dei controller coprono la ricerca di autori e libri, le risorse mancanti, l'autore assente durante l'inserimento di un libro e gli errori di validazione. I test dei service verificano ricerca, conversione in DTO, salvataggio ed eliminazione, comprese le operazioni che non devono raggiungere il repository.

La suite completa è stata eseguita con Java 21, profilo `prod` e database MySQL attivo:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Criteri di accettazione soddisfatti

- I controller sono verificati tramite `MockMvc` e `@WebMvcTest`.
- La struttura JSON standard è controllata nei casi di successo e di errore.
- I service sono isolati tramite Mockito e non dipendono dal database.
- Le interazioni con repository e mapper sono controllate tramite `verify()`.
- L'azzeramento dell'ID dell'autore prima del salvataggio è verificato con `argThat()`.
- La suite completa non ha rilevato regressioni.
