package it.marconi.biblioteca.controllers;

import java.util.List;

import it.marconi.biblioteca.domain.response.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/libri")
public class LibroController {

    private final LibroService libroService;

    @Autowired
    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }


    @GetMapping
    @Operation(summary = "Recupera la lista di tutti i libri")
    public APIResponse<List<LibroDTO>> getAll() {
        return APIResponse.success(libroService.findAll());
    }

    @GetMapping("/{isbn}")
    @Operation(summary = "Cerca un libro dal suo ISBN")
    public APIResponse<LibroDTO> getLibroByIsbn(@PathVariable String isbn) {
        return libroService.getByIsbn(isbn)
                .map(APIResponse::success)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Libro non trovato per ISBN"
                ));
    }

    @GetMapping("/libro")
    @Operation(summary = "Cerca un libro per titolo esatto")
    public APIResponse<LibroDTO> getLibroByTitolo(@RequestParam("titolo") String titolo) {
        return libroService.getByTitolo(titolo)
                .map(APIResponse::success)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Libro non trovato per titolo"
                ));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo libro, dato l'autore")
    public APIResponse<LibroDTO> addLibro(@Valid @RequestBody LibroDTO libro) {
        return libroService.save(libro)
                .map(APIResponse::success)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Autore non trovato"
                ));
    }

    @DeleteMapping("/{isbn}")
    @Operation(summary = "Elimina un libro dato il suo ISBN")
    public APIResponse<String> deleteLibro(@PathVariable String isbn) {
        if (!libroService.deleteByIsbn(isbn)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Libro non trovato per ISBN"
            );
        }

        return APIResponse.success("Libro eliminato correttamente");
    }
}
