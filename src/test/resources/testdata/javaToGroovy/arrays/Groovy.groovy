package testdata.javaToGroovy.arrays

class JavaClass {
    private String[] emptyArray = []
    private String[] stringArray = ["4", "5"]
    private byte[] byteArray = ['F', 'T']

    List<String> stringList = Arrays.asList("a", "b", "c")

    void listAccess() {
        String c = stringList.get(2)
    }
}
