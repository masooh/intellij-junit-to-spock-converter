package junit5;

import org.junit.jupiter.api.*;
import sample.Book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookTest {

    private Book book = new Book();

    @BeforeAll
    static void beforeClass() {
        System.out.println("starting book test");
    }

    @AfterAll
    static void afterClass() {
        System.out.println("end of book test");
    }

    @BeforeEach
    void setUp() {
        book.setTitle("book title");
    }

    @AfterEach
    void tearDown() {
        book = null;
    }

    @Test
    void bookWhatEver() {
        book.setPages(33);

        assertEquals((Integer) 33, book.getPages());
    }

    @Test
    void throwsAcrossLines() {
        assertThrows(IllegalArgumentException.class, () -> book.setPages(-1),
                                "Negative page number must fail");

    }

    @Test
    void throwsWithVariable() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> book.setPages(-1),
                "Negative page number must fail");

        assertEquals("pages must be greater 0", exception.getMessage());
    }

}