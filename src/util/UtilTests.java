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

    private static String replaceSlashWithSeparator(String s)
    {
        if (File.separator.equals("\\")) {
            // Awful hack.  The arguments to 'replaceAll' are
            // regexes or something that has backslash as a
            // metacharacter, so I can't use File.separator
            // directly.  I don't have Java docs here, so I
            // do not know how to escape a string properly
            // in this context.
            return s.replaceAll("/", "\\\\");
        }
        else {
            return s.replaceAll("/", File.separator);
        }
    }

    private void testFileRelativeTo(String base, String name, String expected)
    {
        // My tests are written using "/", but Util.getFileRelativeTo
        // interprets File.separator.  For now, I will just stick in
        // this hack for the tests, so they use File.separator too.
        // It is possible that a better fix is to change the function
        // under test to interpret "/".
        base = replaceSlashWithSeparator(base);
        name = replaceSlashWithSeparator(name);
        expected = replaceSlashWithSeparator(expected);

        File baseFile = new File(base);
        File actual = Util.getFileRelativeTo(baseFile, name);
        File expectedFile = new File(expected);
        assert(actual.equals(expectedFile));
    }
}

// EOF
