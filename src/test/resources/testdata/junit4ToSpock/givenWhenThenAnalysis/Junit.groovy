package testdata.junit4ToSpock.givenWhenThenAnalysis

import org.junit.Test
import sample.Book

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

public class Testcase {

    @Test
    public void assertOnly() {
        assertNotNull(new Book());
    }

    @Test
    public void givenWhenThenAnalysis() {
        // g
        String title = "title";
        Book bookToTest = new Book();
        bookToTest.setPages(10);
        bookToTest.setTitle(title);

        // w
        while (bookToTest.getPages() < 34) {
            bookToTest.setPages(bookToTest.getPages() + 1);
        }

        // t
        assertEquals("title", bookToTest.getTitle());

        int pages = bookToTest.getPages(); // assignment stays in then
        assertEquals(34, pages);

        // w
        Book otherBook = new Book();
        otherBook.setPages(22);

        // t
        assertEquals((Integer) 22, otherBook.getPages());
    }

    /**
     * Last statement has to be considered as assertion, otherwise its no valid test
     */
    @Test
    public void givenExpectAnalysis() {
        // g
        Book book = new Book();

        // e
        book.prefixTitle("blub");
    }
}