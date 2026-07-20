package it.marconi.biblioteca.services;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.AutoreMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoreServiceTest {

    @Mock
    private AutoreRepository autoreRepo;

    @Mock
    private AutoreMapper mapper;

    @InjectMocks
    private AutoreService autoreService;

    @Test
    void saveClearsClientProvidedIdBeforeSaving() {
        AutoreDTO request = new AutoreDTO(99, "Italo", "Calvino");
        Autore mappedAuthor = createAuthor(99, "Italo", "Calvino");
        Autore savedAuthor = createAuthor(1, "Italo", "Calvino");
        AutoreDTO expected = new AutoreDTO(1, "Italo", "Calvino");

        when(mapper.toEntity(request)).thenReturn(mappedAuthor);
        when(autoreRepo.save(any(Autore.class))).thenReturn(savedAuthor);
        when(mapper.toDto(savedAuthor)).thenReturn(expected);

        AutoreDTO result = autoreService.save(request);

        assertEquals(expected, result);
        verify(autoreRepo).save(argThat(author ->
                author == mappedAuthor && author.getId() == null
        ));
        verify(mapper).toDto(savedAuthor);
    }

    @Test
    void findAllMapsEveryAuthorToDto() {
        Autore firstAuthor = createAuthor(1, "Italo", "Calvino");
        Autore secondAuthor = createAuthor(2, "Elsa", "Morante");
        AutoreDTO firstDto = new AutoreDTO(1, "Italo", "Calvino");
        AutoreDTO secondDto = new AutoreDTO(2, "Elsa", "Morante");

        when(autoreRepo.findAll()).thenReturn(List.of(firstAuthor, secondAuthor));
        when(mapper.toDto(firstAuthor)).thenReturn(firstDto);
        when(mapper.toDto(secondAuthor)).thenReturn(secondDto);

        List<AutoreDTO> result = autoreService.findAll();

        assertEquals(List.of(firstDto, secondDto), result);
        verify(mapper).toDto(firstAuthor);
        verify(mapper).toDto(secondAuthor);
    }

    @Test
    void getByIdMapsAuthorWhenItExists() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        AutoreDTO expected = new AutoreDTO(1, "Italo", "Calvino");
        when(autoreRepo.findById(1)).thenReturn(Optional.of(author));
        when(mapper.toDto(author)).thenReturn(expected);

        Optional<AutoreDTO> result = autoreService.getById(1);

        assertEquals(Optional.of(expected), result);
        verify(mapper).toDto(author);
    }

    @Test
    void getByIdReturnsEmptyWhenAuthorDoesNotExist() {
        when(autoreRepo.findById(99)).thenReturn(Optional.empty());

        Optional<AutoreDTO> result = autoreService.getById(99);

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDto(any(Autore.class));
    }

    @Test
    void deleteByIdDeletesAuthorWhenItExists() {
        when(autoreRepo.existsById(1)).thenReturn(true);

        boolean result = autoreService.deleteById(1);

        assertTrue(result);
        verify(autoreRepo).deleteById(1);
    }

    @Test
    void deleteByIdDoesNotDeleteWhenAuthorDoesNotExist() {
        when(autoreRepo.existsById(99)).thenReturn(false);

        boolean result = autoreService.deleteById(99);

        assertFalse(result);
        verify(autoreRepo, never()).deleteById(99);
    }

    private Autore createAuthor(Integer id, String name, String surname) {
        Autore author = new Autore();
        author.setId(id);
        author.setNome(name);
        author.setCognome(surname);
        return author;
    }
}
