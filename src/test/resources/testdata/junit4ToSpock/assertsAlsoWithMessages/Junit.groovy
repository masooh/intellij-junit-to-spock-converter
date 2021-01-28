package testdata.junit4ToSpock.assertsAlsoWithMessages

import org.junit.Test
import sample.Book

import static org.junit.Assert.*

public class Testcase {
    private Book book = new Book();

    @Test
    public void assertsAlsoWithMessages() {
        assertNotNull(book);
        assertNotNull("book is present", book);
        assertNull(book.getPages());
        assertNull("pages initial state is null", book.getPages());

        book.setPages(33);

        assertEquals((Integer) 33, book.getPages());
        assertEquals("pages set to 33", (Integer) 33, book.getPages());

        assertTrue(book.getPages() > 0);
        assertTrue("pages must not be negative", book.getPages() > 0);
        assertFalse(book.getPages() < 0);
        assertFalse("pages must not be negative", book.getPages() < 0);
    }
}