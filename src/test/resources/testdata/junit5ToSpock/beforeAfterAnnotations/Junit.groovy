package testdata.junit5ToSpock.beforeAfterAnnotations

import org.junit.jupiter.api.*

import static org.junit.jupiter.api.Assertions.assertTrue

public class Testcase {
    @BeforeAll
    static void beforeClass() {
        System.out.println("before class");
    }

    @AfterAll
    static void afterClass() {
        System.out.println("after class");
    }

    @BeforeEach
    void setUp() {
        System.out.println("before each");
    }

    @AfterEach
    void tearDown() {
        System.out.println("after each");
    }

    @Test
    void alwaysTrue() {
        assertTrue(true)
    }
}