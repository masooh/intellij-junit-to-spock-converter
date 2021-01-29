package testdata.junit5ToSpock.assertions


import sample.Book
import spock.lang.Specification

class Testcase extends Specification {
    private Book book = new Book()


    def "book what ever"() {
        when:
        book.pages = 33

        then:
        book.pages == ( Integer) 33
    }


    def "expect argument exception single lines"() {
        when:
        book.pages = -1

        then:
        thrown(IllegalArgumentException)

    }


    def "expect argument exception across lines"() {
        when:
        book.pages = -1

        then: "Negative page number must fail"
        thrown(IllegalArgumentException)
    }


    def "throws with variable"() {
        when:
        book.pages = -1

        then: "Negative page number must fail"
        Exception exception = thrown(IllegalArgumentException)
        exception.message == "pages must be greater 0"
    }
}