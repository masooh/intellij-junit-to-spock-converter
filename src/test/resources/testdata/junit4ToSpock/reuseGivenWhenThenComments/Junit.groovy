package testdata.junit4ToSpock.reuseGivenWhenThenComments

import org.junit.Test
import sample.Book

import static org.junit.Assert.assertEquals

public class Testcase {

    @Test
    public void givenWhenThenComments() {
        // given
        Book bookToTest = new Book();

        // when
        bookToTest.setTitle("title");

        // then
        assertEquals("title", bookToTest.getTitle());
    }
}