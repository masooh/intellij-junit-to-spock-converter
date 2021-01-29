package testdata.junit5ToSpock.beforeAfterAnnotations


import spock.lang.Specification

class Testcase extends Specification {

    def setupSpec() {
        System.out.println("before class")
    }


    def cleanupSpec() {
        System.out.println("after class")
    }


    def setup() {
        System.out.println("before each")
    }


    def cleanup() {
        System.out.println("after each")
    }


    def "always true"() {
        expect: true
    }
}