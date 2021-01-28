package testdata.junit4ToSpock.beforeAfterMethods

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

public class Testcase {

    @BeforeClass
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("afterClass");
    }

    @Before
    public void setUp() {
        System.out.println("before");
    }

    @After
    public void tearDown() {
        System.out.println("after");
    }
}