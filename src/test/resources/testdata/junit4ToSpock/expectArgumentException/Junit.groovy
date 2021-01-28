package testdata.junit4ToSpock.expectArgumentException

import org.junit.Test
import sample.Book

public class Testcase {
    Book book = new Book()

    @Test(expected = IllegalArgumentException.class)
    public void expectArgumentException() {
        book.setPages(-1);
    }
}