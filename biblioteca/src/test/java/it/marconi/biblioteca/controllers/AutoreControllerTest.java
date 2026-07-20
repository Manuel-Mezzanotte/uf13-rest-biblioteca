package it.marconi.biblioteca.controllers;

import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.services.AutoreService;
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

@WebMvcTest(AutoreController.class)
class AutoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutoreService autoreService;

    @MockitoBean
    private LibroService libroService;

    @Test
    void getAutoreReturnsStandardResponseWhenAuthorExists() throws Exception {
        AutoreDTO autore = new AutoreDTO(1, "Italo", "Calvino");
        when(autoreService.getById(1)).thenReturn(Optional.of(autore));

        mockMvc.perform(get("/autori/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Italo"))
                .andExpect(jsonPath("$.data.cognome").value("Calvino"));
    }

    @Test
    void getAutoreReturnsStandardNotFoundResponseWhenAuthorDoesNotExist() throws Exception {
        when(autoreService.getById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/autori/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Autore non trovato"))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void addAutoreReturnsFieldErrorsWhenRequestIsInvalid() throws Exception {
        String invalidAuthor = """
                {
                  "id": 15,
                  "nome": "",
                  "cognome": ""
                }
                """;

        mockMvc.perform(post("/autori/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidAuthor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Errore nella validazione dei dati"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.nome").value("Il nome è obbligatorio"))
                .andExpect(jsonPath("$.data.cognome").value("Il cognome è obbligatorio"));

        verify(autoreService, never()).save(any(AutoreDTO.class));
    }
}
