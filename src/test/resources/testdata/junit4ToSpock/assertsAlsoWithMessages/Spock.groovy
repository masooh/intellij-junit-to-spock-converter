package testdata.junit4ToSpock.assertsAlsoWithMessages


import sample.Book
import spock.lang.Specification

class Testcase extends Specification {
    private Book book = new Book()


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
}