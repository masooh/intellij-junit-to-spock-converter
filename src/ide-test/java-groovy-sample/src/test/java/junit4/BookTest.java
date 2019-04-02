package junit4;

import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        // w
        String title = "title";
        Book bookToTest = new Book();
        bookToTest.setTitle(title);

        while (bookToTest.getPages() < 34) {
            bookToTest.setPages(bookToTest.getPages() + 1);
        }

        // t
        assertEquals("title", bookToTest.getTitle());

        int pages = bookToTest.getPages(); // assignment stays in then
        assertEquals(33, pages);

        // w
        bookToTest.setPages(22);

        // t
        assertEquals((Integer) 22, bookToTest.getPages());
    }

    // TODO Test ohne assert, aber mit expected exception -> kein expect sondern when -> thrown zählt wie assert

    // TODO Test mit @RunWith + @ContextConfig ... { ... }
}