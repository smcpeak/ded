// Util.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import util.StringUtil;

import static util.StringUtil.fmt;


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

    /** Return -1/0/+1 for 'a' lt/eq/gt 'b' where 'null' comes before
      * non-null. */
    public static int compareNullability(Object a, Object b)
    {
        if (a == null) {
            return b==null? 0 : -1;
        }
        else {
            return b==null? +1 : 0;
        }
    }

    /** Return 'coll' in a sorted array. */
    public static <T extends Comparable<? super T> >
    ArrayList<T> sorted(Collection<T> coll)
    {
        ArrayList<T> arr = new ArrayList<T>();

        for (T t : coll) {
            arr.add(t);
        }

        Collections.sort(arr);
        return arr;
    }

    /** Read all of 'stream' into a single string. */
    public static String readStreamAsString(InputStreamReader stream)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        while (true) {
            int len = stream.read(buffer);
            if (len > 0) {
                sb.append(buffer, 0, len);
            }
            else {
                break;
            }
        }
        return sb.toString();
    }

    /** Read a resource file as a string. */
    public static String readResourceString(String fname)
    {
        InputStream is = Util.class.getResourceAsStream(fname);
        if (is != null) {
            try {
                return readStreamAsString(
                  new InputStreamReader(is, "UTF-8"));
            }
            catch (IOException e) {
                return fmt("[Error while reading resource \"%1$s\": %2$s]",
                           fname, Util.getExceptionMessage(e));
            }
            finally {
                try {
                    is.close();
                }
                catch (IOException e) {/*ignore*/}
            }
        }
        else {
            return fmt("[Missing resource: \"%1$s\".]", fname);
        }
    }

    /** Read a resource file, then join its adjacent lines before
      * returning the result. */
    public static String readResourceString_joinAdjacentLines(
        String fname)
    {
        return StringUtil.joinAdjacentLines(readResourceString(fname));
    }

    /** Provide an implementation of 'equals' in terms of 'compareTo'. */
    public static <T extends Comparable<? super T> >
    boolean equalsViaCompare(T a, Object b)
    {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }
        if (a.getClass() != b.getClass()) {
            return false;
        }
        return a.compareTo((T)b) == 0;
    }

    /** True if 'a' and 'b' are disjoint. */
    public static <T>
    boolean disjointSets(Set<T> a, Set<T> b)
    {
        for (T t : a) {
            if (b.contains(t)) {
                return false;
            }
        }
        return true;
    }

    /** True if 'fname' exists.  Beware it could be a directory or a
        special file. */
    public static boolean filenameExists(String fname)
    {
        File f = new File(fname);
        return f.exists();
    }
}

// EOF
