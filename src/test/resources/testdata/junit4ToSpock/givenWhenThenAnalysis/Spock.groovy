package testdata.junit4ToSpock.givenWhenThenAnalysis


import sample.Book
import spock.lang.Specification

class Testcase extends Specification {


    def "assert only"() {
        expect:
        new Book() != null
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
        Book otherBook = new Book()
        otherBook.pages = 22

        // t
        then:
        otherBook.pages == (Integer) 22
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
}