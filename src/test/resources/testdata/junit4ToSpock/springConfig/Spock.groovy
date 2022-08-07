package testdata.junit4ToSpock.springConfig


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import sample.Book
import spock.lang.Specification

@ContextConfiguration(classes = SpringTest.Config)
class SpringTest extends Specification {

    static class Config {
        @Bean
        Book cleanCode() {
            Book book = new Book()
            book.title = 'Clean Code'
            return book
        }
    }

    @Autowired
    Book book


    def "injection works"() {
        expect:
        book != null

        book.title == 'Clean Code'
    }
}