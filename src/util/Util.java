// Util.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

/** Generic Java utilities. */
public class Util {
    /** Compute hash code for a collection. */
    public static <T> int collectionHashCode(Collection<T> coll)
    {
        if (coll == null) {
            return 0;
        }

        int h = 1;
        for (T t : coll) {
            h = h*31 + t.hashCode();
        }
        return h;
    }

    /** Compute the average of two integers without overflow. */
    public static int avg(int a, int b)
    {
        // Clever solution from here:
        // http://code-o-matic.blogspot.com/2010/02/find-average-between-two-ints-facing.html
        //
        // This works because a&b has all the bits where 'a' and 'b'
        // agree, and a^b has where they differ.  But where they differ,
        // you can swap the bits without affecting the sum, and hence
        // the average.  Therefore, you can transform the pair (a,b) to
        // the pair ((a&b),(a&b)+(a^b)) one bit at a time w/o changing
        // the average.
        //
        // Now, (a&b)+(a^b) cannot overflow since it equals (a&b)|(a^b),
        // since no bits are in common, which means that adding a number
        // closer to 0 also cannot overflow.  The return expression
        // is the average of the transformed pair, and will not overflow.
        return (a&b) + (a^b)/2;
    }

    /** Return true if 'v' is NaN or one of the infinities. */
    public static boolean isSpecialDouble(double v)
    {
        return Double.isInfinite(v) || Double.isNaN(v);
    }

    /** Return true if 'a' and 'b' are both null or are both not
      * null and equals(). */
    public static boolean nullableEquals(Object a, Object b)
    {
        if (a == null) {
            return b == null;
        }
        else if (b == null) {
            return false;
        }
        else {
            return a.equals(b);
        }
    }

    /** Return 0 if 'o' is null, else o.hashCode(). */
    public static int nullableHashCode(Object o)
    {
        if (o == null) {
            return 0;
        }
        else {
            return o.hashCode();
        }
    }

    /** Return the current working directory as a File. */
    public static File getWorkingDirectoryFile()
    {
        return new File(System.getProperty("user.dir"));
    }

    /** If 'name' is absolute, then just return it as a File.  Otherwise,
      * interpret it as relative to 'relativeBase' and return that
      * combination as a File. */
    public static File getFileRelativeTo(File relativeBase, String name)
    {
        if (name.startsWith(File.separator)) {
            return new File(name);
        }
        else {
            return new File(relativeBase, name);
        }
    }

    /** Make a copy of the input array, which may be null, in which case
      * the return value is null too. */
    public static int[] copyArray(int[] src)
    {
        if (src == null) {
            return null;
        }
        else {
            return Arrays.copyOf(src, src.length);
        }
    }

    /** Given an Exception, extract a human-readable error message. */
    public static String getExceptionMessage(Exception e)
    {
        // Many Java exceptions, especially those in the standard
        // libraries, only convey the conflict in the name of the
        // exception class.  That is a really bad practice, but I
        // have to live with it.
        String conflict = e.getClass().getSimpleName();

        // Even so, some of the class names are misleading, so
        // fix them.
        if (e instanceof FileNotFoundException) {
            // This really mean any failure to open a file,
            // including permission errors.
            conflict = "Could not open file";
        }

        return conflict+": "+e.getMessage();
    }

    /** Given an Exception, get its stack trace as a string. */
    public static String getExceptionStackTrace(Exception e)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    // strcmp-style comparison of integers.
    public static int compareInts(int a, int b)
    {
        return a < b ? -1 :
               a > b ? +1 :
                        0 ;
    }
}

// EOF
