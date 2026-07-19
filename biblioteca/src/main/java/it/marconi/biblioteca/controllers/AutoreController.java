package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.domain.response.APIResponse;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/autori")
public class AutoreController {

    private final LibroService libroService;
    private final AutoreService autoreService;

    @Autowired
    public AutoreController(LibroService libroService, AutoreService autoreService) {
        this.libroService = libroService;
        this.autoreService = autoreService;
    }

    @GetMapping
    @Operation(summary = "Recupera tutti gli autori")
    public APIResponse<List<AutoreDTO>> getAll() {
        return APIResponse.success(autoreService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Cerca un autore dato il suo ID")
    public APIResponse<AutoreDTO> getAutore(@PathVariable Integer id) {
        return autoreService.getById(id)
                .map(APIResponse::success)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Autore non trovato"
                ));
    }

    @GetMapping("/{id}/libri")
    @Operation(summary = "Recupera tutti i libri di un dato autore")
    public APIResponse<List<LibroDTO>> getLibriByAutore(@PathVariable Integer id) {
        autoreService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Autore non trovato"
                ));

        return APIResponse.success(libroService.getByAutoreId(id));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo autore")
    public APIResponse<AutoreDTO> addAutore(@Valid @RequestBody AutoreDTO autore) {
        return APIResponse.success(autoreService.save(autore));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuove un autore dal database, e anche tutti i suoi libri")
    public APIResponse<String> deleteAutore(@PathVariable Integer id) {
        if (!autoreService.deleteById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Autore non trovato"
            );
        }

        return APIResponse.success("Autore eliminato correttamente");
    }
}
