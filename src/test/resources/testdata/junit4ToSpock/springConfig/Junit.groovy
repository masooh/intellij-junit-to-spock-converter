package testdata.junit4ToSpock.springConfig

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import sample.Book

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringTest.Config.class)
public class SpringTest {

    static class Config {
        @Bean
        Book cleanCode() {
            Book book = new Book();
            book.setTitle("Clean Code");
            return book;
        }
    }

    @Autowired
    Book book;

    @Test
    public void injectionWorks() {
        Assert.assertNotNull(book);

        Assert.assertEquals("Clean Code", book.getTitle());
    }
}