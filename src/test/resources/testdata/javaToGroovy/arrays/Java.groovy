package testdata.javaToGroovy.arrays

public class JavaClass {
    private String[] emptyArray = new String[] {};
    private String[] stringArray = new String[] { "4", "5" };
    private byte[] byteArray = new byte[]{'F', 'T'};

    List<String> stringList = Arrays.asList("a", "b", "c");

    public void listAccess() {
        String c = stringList.get(2);
    }
}
