package junit5;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sample.Book;

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
    void expectArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> book.setPages(-1),
                                "Negative page number must fail");

    }

}