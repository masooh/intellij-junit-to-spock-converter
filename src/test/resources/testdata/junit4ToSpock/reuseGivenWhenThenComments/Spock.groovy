package testdata.junit4ToSpock.reuseGivenWhenThenComments


import sample.Book
import spock.lang.Specification

class Testcase extends Specification {


    def "given when then comments"() {
        given:
        Book bookToTest = new Book()

        when:
        bookToTest.title = "title"

        then:
        bookToTest.title == "title"
    }
}