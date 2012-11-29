// UtilTests.java

package util;

import java.io.File;

/** Unit tests for the 'Util' class. */
public class UtilTests {
    public static void main(String args[])
    {
        UtilTests ut = new UtilTests();
        ut.testFileRelativeTo("/a", "b", "/a/b");
        ut.testFileRelativeTo("/a", "/b", "/b");
        ut.testFileRelativeTo("/a", "b/c", "/a/b/c");
        ut.testFileRelativeTo("/a", "/b/c", "/b/c");
        ut.testFileRelativeTo("/a/b", "../c", "/a/b/../c");
    }
    
    private void testFileRelativeTo(String base, String name, String expected)
    {
        File baseFile = new File(base);
        File actual = Util.getFileRelativeTo(baseFile, name);
        File expectedFile = new File(expected);
        assert(actual.equals(expectedFile));
    }
}

// EOF
