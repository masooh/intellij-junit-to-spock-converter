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
    public void asserts() {
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

    // TODO Test ohne assert, aber mit expected exception -> kein expect sondern when -> thrown zählt wie assert

    // TODO Test mit @RunWith + @ContextConfig ... { ... }
}