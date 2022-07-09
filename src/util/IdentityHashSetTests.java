// IdentityHashSetTests.java
// See toplevel license.txt for copyright and license terms.

package util;

/** Tests for IdentityHashSet. */
public class IdentityHashSetTests {
    public static void main(String args[])
    {
        // Use two distinct objects that have the same value to test
        // that the container is using object identity rather than value
        // identity.
        //
        // Originally, this test used Integer objects, but the required
        // constructor in that case is now deprecated, so I switched to
        // String, although the locals like "i1" are still named as if
        // they were integers.
        String i1 = new String("1");
        String i2 = new String("1");

        assert(i1.equals(i2));
        assert(i1 != i2);

        IdentityHashSet<String> s = new IdentityHashSet<String>();
        assert(s.isEmpty());
        assert(s.size() == 0);
        assert(!s.contains(i1));
        assert(!s.contains(i2));
        for (@SuppressWarnings("unused") String i : s) {
            assert(false);   // should not have any elements
        }

        assert(s.add(i1) == true);
        assert(!s.isEmpty());
        assert(s.size() == 1);
        assert(s.contains(i1));
        assert(!s.contains(i2));
        for (String i : s) {
            assert(i == i1);
        }

        assert(s.add(i2) == true);
        assert(s.size() == 2);
        assert(s.contains(i1));
        assert(s.contains(i2));
        for (String i : s) {
            assert(i == i1 || i == i2);
        }

        assert(s.add(i2) == false);
        assert(s.size() == 2);

        assert(s.remove(i1) == true);
        assert(s.size() == 1);
        assert(!s.contains(i1));
        assert(s.contains(i2));
        for (String i : s) {
            assert(i == i2);
        }
    }
}
