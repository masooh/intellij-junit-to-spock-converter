package junit4;

import org.junit.*;

import static org.junit.Assert.assertEquals;

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
    public void bookWhatEver() {
        book.setPages(33);

        assertEquals((Integer) 33, book.getPages());
    }

    @Test(expected = IllegalArgumentException.class)
    public void expectArgumentException() {
        book.setPages(-1);
    }

    // todo assertNotNull, ... alle VArianten einpflegen

    // TODO Test mit nur asserts

    // TODO Test ohne assert, aber mit expected exception -> kein expect sondern when -> thrown zählt wie assert

    // TODO Test mit @RunWith + @ContextConfig ... { ... }
}