package junit4


import sample.Book
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class BookTest extends Specification {

    private Book book = new Book()


    def setupSpec() {
        System.out.println("starting book test")
    }


    def cleanupSpec() {
        System.out.println("end of book test")
    }


    def setup() {
        book.title = "book title"
    }


    def cleanup() {
        book = null
    }


    def "asserts also with messages"() {
        expect:
        book != null
        book != null // "book is present"

        book.pages == null
        book.pages == null // "pages initial state is null"


        when:
        book.pages = 33

        then:
        book.pages == (Integer) 33
        book.pages == (Integer) 33 // "pages set to 33"


        book.pages > 0
        book.pages > 0 // "pages must not be negative"

        !(book.pages < 0)
        !(book.pages < 0) // "pages must not be negative"

    }


    def "assert only"() {
        expect:
        book != null
    }


    def "expect argument exception"() {
        when:
        book.pages = -1
        then:
        thrown(IllegalArgumentException)
    }


    def "given when then comments"() {
        // given
        given:
        Book bookToTest = new Book()

        // when
        when:
        bookToTest.title = "title"

        // then
        then:
        bookToTest.title == "title"
    }


    def "given when then analysis"() {
        // g
        given:
        String title = "title"
        Book bookToTest = new Book()
        bookToTest.pages = 10
        bookToTest.title = title

        // w
        when:
        while (bookToTest.pages < 34) {
            bookToTest.pages = bookToTest.pages + 1
        }

        // t
        then:
        bookToTest.title == "title"

        int pages = bookToTest.pages // assignment stays in then
        pages == 34

        // w
        when:
        bookToTest.pages = 22

        // t
        then:
        bookToTest.pages == (Integer) 22
    }


    def "unknown assert must not cause exception"() {
        expect:
        assertDoesNotExist()
    }

    /**
     * Last statement has to be considered as assertion, otherwise its no valid test
     */

    def "given expect analysis"() {
        // g
        given:
        Book book = new Book()

        // e
        expect:
        book.prefixTitle("blub")
    }

    /** replace only if it is just one loop at the end */

    def "assertion in loop"() {
        given:
        Book first = new Book()
        first.pages = 10

        Book second = new Book()
        second.pages = 10

        final List<Book> books = Arrays.asList(first, second)

        // where
        expect:
        for (Book bookToTest : books) {
            // then
            assertEquals((Integer)10, bookToTest.pages)
            assertNull(bookToTest.title)
        }
    }


    def "similar asserts to data driven"() {
        expect:
        createBook("title1").prefixTitle("pre") == "pre title1"
        createBook("title2").prefixTitle("") == " title2"
        createBook("title3").prefixTitle("prefix") == "prefix title3"
    }

    private Book createBook(String title) {
        final Book book = new Book()
        book.title = title
        return book
    }

    private void assertDoesNotExist() {

    }

    // TODO Test ohne assert, aber mit expected exception -> kein expect sondern when -> thrown zählt wie assert

    // TODO Test mit @RunWith + @ContextConfig ... { ... }
}