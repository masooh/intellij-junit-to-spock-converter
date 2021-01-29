package testdata.junit5ToSpock.assertions

import org.junit.jupiter.api.Test
import sample.Book

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

public class Testcase {
    private Book book = new Book();

    @Test
    void bookWhatEver() {
        book.setPages(33);

        assertEquals(( Integer) 33, book.getPages());
    }

    @Test
    void expectArgumentExceptionSingleLines() {
        assertThrows(IllegalArgumentException.class, () -> book.setPages(-1));

    }

    @Test
    void expectArgumentExceptionAcrossLines() {
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