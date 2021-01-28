package testdata.junit4ToSpock.expectArgumentException


import sample.Book
import spock.lang.Specification

class Testcase extends Specification {
    Book book = new Book()


    def "expect argument exception"() {
        when:
        book.pages = -1
        then:
        thrown(IllegalArgumentException.class)
    }
}