package it.marconi.biblioteca.services;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.Libro;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.domain.LibroMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;
import it.marconi.biblioteca.repositories.LibroRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepo;

    @Mock
    private AutoreRepository autoreRepo;

    @Mock
    private LibroMapper mapper;

    @InjectMocks
    private LibroService libroService;

    @Test
    void findAllMapsEveryBookToDto() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        Libro firstBook = createBook("9788804668237", "Il barone rampante", author);
        Libro secondBook = createBook("9788804771357", "Le città invisibili", author);
        LibroDTO firstDto = createBookDto("9788804668237", "Il barone rampante", 1);
        LibroDTO secondDto = createBookDto("9788804771357", "Le città invisibili", 1);

        when(libroRepo.findAll()).thenReturn(List.of(firstBook, secondBook));
        when(mapper.toDto(firstBook)).thenReturn(firstDto);
        when(mapper.toDto(secondBook)).thenReturn(secondDto);

        List<LibroDTO> result = libroService.findAll();

        assertEquals(List.of(firstDto, secondDto), result);
        verify(mapper).toDto(firstBook);
        verify(mapper).toDto(secondBook);
    }

    @Test
    void getByIsbnMapsBookWhenItExists() {
        String isbn = "9788804668237";
        Autore author = createAuthor(1, "Italo", "Calvino");
        Libro book = createBook(isbn, "Il barone rampante", author);
        LibroDTO expected = createBookDto(isbn, "Il barone rampante", 1);
        when(libroRepo.findById(isbn)).thenReturn(Optional.of(book));
        when(mapper.toDto(book)).thenReturn(expected);

        Optional<LibroDTO> result = libroService.getByIsbn(isbn);

        assertEquals(Optional.of(expected), result);
        verify(mapper).toDto(book);
    }

    @Test
    void getByIsbnReturnsEmptyWhenBookDoesNotExist() {
        String isbn = "9788804668237";
        when(libroRepo.findById(isbn)).thenReturn(Optional.empty());

        Optional<LibroDTO> result = libroService.getByIsbn(isbn);

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDto(any(Libro.class));
    }

    @Test
    void getByTitoloMapsBookWithExactTitle() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        Libro otherBook = createBook("9788804771357", "Le città invisibili", author);
        Libro matchingBook = createBook("9788804668237", "Il barone rampante", author);
        LibroDTO expected = createBookDto("9788804668237", "Il barone rampante", 1);
        when(libroRepo.findAll()).thenReturn(List.of(otherBook, matchingBook));
        when(mapper.toDto(matchingBook)).thenReturn(expected);

        Optional<LibroDTO> result = libroService.getByTitolo("Il barone rampante");

        assertEquals(Optional.of(expected), result);
        verify(mapper).toDto(matchingBook);
        verify(mapper, never()).toDto(otherBook);
    }

    @Test
    void getByTitoloReturnsEmptyWhenBookDoesNotExist() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        Libro book = createBook("9788804771357", "Le città invisibili", author);
        when(libroRepo.findAll()).thenReturn(List.of(book));

        Optional<LibroDTO> result = libroService.getByTitolo("Titolo inesistente");

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDto(any(Libro.class));
    }

    @Test
    void getByAutoreIdMapsBooksReturnedByRepository() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        Libro book = createBook("9788804668237", "Il barone rampante", author);
        LibroDTO expected = createBookDto("9788804668237", "Il barone rampante", 1);
        when(libroRepo.findByAutoreId(1)).thenReturn(List.of(book));
        when(mapper.toDto(book)).thenReturn(expected);

        List<LibroDTO> result = libroService.getByAutoreId(1);

        assertEquals(List.of(expected), result);
        verify(libroRepo).findByAutoreId(1);
        verify(mapper).toDto(book);
    }

    @Test
    void savePersistsAndMapsBookWhenAuthorExists() {
        Autore author = createAuthor(1, "Italo", "Calvino");
        LibroDTO request = createBookDto("9788804668237", "Il barone rampante", 1);
        Libro mappedBook = createBook("9788804668237", "Il barone rampante", author);
        Libro savedBook = createBook("9788804668237", "Il barone rampante", author);
        LibroDTO expected = createBookDto("9788804668237", "Il barone rampante", 1);

        when(autoreRepo.findById(1)).thenReturn(Optional.of(author));
        when(mapper.toEntity(request, author)).thenReturn(mappedBook);
        when(libroRepo.save(mappedBook)).thenReturn(savedBook);
        when(mapper.toDto(savedBook)).thenReturn(expected);

        Optional<LibroDTO> result = libroService.save(request);

        assertEquals(Optional.of(expected), result);
        verify(mapper).toEntity(request, author);
        verify(libroRepo).save(mappedBook);
        verify(mapper).toDto(savedBook);
    }

    @Test
    void saveDoesNotPersistBookWhenAuthorDoesNotExist() {
        LibroDTO request = createBookDto("9788804668237", "Il barone rampante", 99);
        when(autoreRepo.findById(99)).thenReturn(Optional.empty());

        Optional<LibroDTO> result = libroService.save(request);

        assertTrue(result.isEmpty());
        verifyNoInteractions(libroRepo, mapper);
    }

    @Test
    void deleteByIsbnDeletesBookWhenItExists() {
        String isbn = "9788804668237";
        when(libroRepo.existsById(isbn)).thenReturn(true);

        boolean result = libroService.deleteByIsbn(isbn);

        assertTrue(result);
        verify(libroRepo).deleteById(isbn);
    }

    @Test
    void deleteByIsbnDoesNotDeleteWhenBookDoesNotExist() {
        String isbn = "9788804668237";
        when(libroRepo.existsById(isbn)).thenReturn(false);

        boolean result = libroService.deleteByIsbn(isbn);

        assertFalse(result);
        verify(libroRepo, never()).deleteById(isbn);
    }

    private Autore createAuthor(Integer id, String name, String surname) {
        Autore author = new Autore();
        author.setId(id);
        author.setNome(name);
        author.setCognome(surname);
        return author;
    }

    private Libro createBook(String isbn, String title, Autore author) {
        Libro book = new Libro();
        book.setIsbn(isbn);
        book.setTitolo(title);
        book.setGenere("Romanzo");
        book.setAnno(1957);
        book.setAutore(author);
        return book;
    }

    private LibroDTO createBookDto(String isbn, String title, Integer authorId) {
        return new LibroDTO(isbn, title, "Romanzo", 1957, authorId);
    }
}
