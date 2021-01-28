package testdata.junit4ToSpock.classAndFeature


import spock.lang.Specification

class Testcase extends Specification {


    def "always true"() {
        expect: true
    }
}