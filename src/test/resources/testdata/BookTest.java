package junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sample.Book;

public class BookTest {

    private Book book = new Book();

    @BeforeClass
    public static void beforeClass() {
        System.out.println("starting book test");
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("end of book test");
    }

    @Before
    public void setUp() {
        book.setTitle("book title");
    }

    @After
    public void tearDown() {
        book = null;
    }

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

    @Test
    public void assertOnly() {
        assertNotNull(book);
    }

    @Test(expected = IllegalArgumentException.class)
    public void expectArgumentException() {
        book.setPages(-1);
    }

    @Test
    public void givenWhenThenComments() {
        // given
        Book bookToTest = new Book();

        // when
        bookToTest.setTitle("title");

        // then
        assertEquals("title", bookToTest.getTitle());
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
        bookToTest.setPages(22);

        // t
        assertEquals((Integer) 22, bookToTest.getPages());
    }

    @Test
    public void unknownAssertMustNotCauseException() {
        assertDoesNotExist();
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

    /** replace only if it is just one loop at the end */
    @Test
    public void assertionInLoop() {
        Book first = new Book();
        first.setPages(10);

        Book second = new Book();
        second.setPages(10);

        final List<Book> books = Arrays.asList(first, second);

        // where
        for (Book bookToTest : books) {
            // then
            assertEquals((Integer)10, bookToTest.getPages());
            assertNull(bookToTest.getTitle());
        }
    }

    @Test
    public void similarAssertsToDataDriven() {
        assertEquals("pre title1", createBook("title1").prefixTitle("pre"));
        assertEquals(" title2", createBook("title2").prefixTitle(""));
        assertEquals("prefix title3", createBook("title3").prefixTitle("prefix"));
    }

    private Book createBook(String title) {
        final Book book = new Book();
        book.setTitle(title);
        return book;
    }

    private void assertDoesNotExist() {

    }

    // TODO Test ohne assert, aber mit expected exception -> kein expect sondern when -> thrown zählt wie assert

    // TODO Test mit @RunWith + @ContextConfig ... { ... }
}