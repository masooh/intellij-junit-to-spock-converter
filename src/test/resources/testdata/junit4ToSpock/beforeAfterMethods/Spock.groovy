package testdata.junit4ToSpock.beforeAfterMethods


import spock.lang.Specification

class Testcase extends Specification {


    def setupSpec() {
        System.out.println("beforeClass")
    }


    def cleanupSpec() {
        System.out.println("afterClass")
    }


    def setup() {
        System.out.println("before")
    }


    def cleanup() {
        System.out.println("after")
    }
}