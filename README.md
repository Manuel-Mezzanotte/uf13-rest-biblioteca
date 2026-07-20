# Spring Biblioteca

Webservice REST in Springboot per la gestione di una biblioteca

## Dependencies
- Spring Web
- Spring DevTools
- Spring Data JPA
- MySQL Driver
- Validator
- Lombok
- SpringDoc Open API

## Swagger UI

Swagger è un tool via browser che permette il testing delle nostre API.  

L'interfaccia Swagger è disponibile al seguente indirizzo:

```
http://localhost:8080/swagger-ui/index.html
```

## Frontend web

Il progetto include una semplice interfaccia per gestire libri e autori direttamente dal browser. Per avviare tutti i servizi:

```bash
cd biblioteca
docker compose up -d --build
```

Il frontend è disponibile all'indirizzo `http://localhost:8080/` e permette di aggiungere, cercare ed eliminare libri e autori.

## Documentazione esame UF13

La relazione tecnica è disponibile in [docs/relazione-tecnica.md](docs/relazione-tecnica.md).
