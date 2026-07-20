package it.marconi.biblioteca.controllers;

import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.LibroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibroController.class)
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    @Test
    void getLibroReturnsStandardResponseWhenBookExists() throws Exception {
        String isbn = "9788804668237";
        LibroDTO libro = new LibroDTO(isbn, "Il barone rampante", "Romanzo", 1957, 1);
        when(libroService.getByIsbn(isbn)).thenReturn(Optional.of(libro));

        mockMvc.perform(get("/libri/{isbn}", isbn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.isbn").value(isbn))
                .andExpect(jsonPath("$.data.titolo").value("Il barone rampante"))
                .andExpect(jsonPath("$.data.genere").value("Romanzo"))
                .andExpect(jsonPath("$.data.anno").value(1957))
                .andExpect(jsonPath("$.data.autore").value(1));
    }

    @Test
    void getLibroReturnsStandardNotFoundResponseWhenBookDoesNotExist() throws Exception {
        String isbn = "9788804668237";
        when(libroService.getByIsbn(isbn)).thenReturn(Optional.empty());

        mockMvc.perform(get("/libri/{isbn}", isbn))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Libro non trovato per ISBN"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void addLibroReturnsNotFoundWhenAuthorDoesNotExist() throws Exception {
        String validBook = """
                {
                  "isbn": "9788804668237",
                  "titolo": "Il barone rampante",
                  "genere": "Romanzo",
                  "anno": 1957,
                  "autore": 99
                }
                """;
        when(libroService.save(any(LibroDTO.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/libri/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBook))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Autore non trovato"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void addLibroReturnsFieldErrorsWhenRequestIsInvalid() throws Exception {
        String invalidBook = """
                {
                  "isbn": "",
                  "titolo": "",
                  "genere": "Romanzo",
                  "anno": 1957,
                  "autore": null
                }
                """;

        mockMvc.perform(post("/libri/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Errore nella validazione dei dati"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.isbn").value("ISBN campo obbligatorio"))
                .andExpect(jsonPath("$.data.titolo").value("Il titolo non può essere vuoto"))
                .andExpect(jsonPath("$.data.autore").value("ID autore obbligatorio"));

        verify(libroService, never()).save(any(LibroDTO.class));
    }
}
