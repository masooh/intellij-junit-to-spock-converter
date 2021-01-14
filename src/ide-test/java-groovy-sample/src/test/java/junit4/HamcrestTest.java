package junit4;

import org.junit.Test;
import sample.Book;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class HamcrestTest {
    private Book book = new Book();

    @Test
    public void matchers() {
        assertThat(book, is(notNullValue()));
        assertThat(book, is(not(nullValue())));
        assertThat(book.getPages(), is(nullValue()));

        book.setPages(33);

        assertThat(book.getPages(), equalTo(33));
    }
}