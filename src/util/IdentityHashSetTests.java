// IdentityHashSetTests.java
// See toplevel license.txt for copyright and license terms.

package util;

/** Tests for IdentityHashSet. */
public class IdentityHashSetTests {
    public static void main(String args[])
    {
        Integer i1 = new Integer(1);
        Integer i2 = new Integer(1);
        assert(i1.equals(i2));
        assert(i1 != i2);

        IdentityHashSet<Integer> s = new IdentityHashSet<Integer>();
        assert(s.isEmpty());
        assert(s.size() == 0);
        assert(!s.contains(i1));
        assert(!s.contains(i2));
        for (@SuppressWarnings("unused") Integer i : s) {
            assert(false);   // should not have any elements
        }

        assert(s.add(i1) == true);
        assert(!s.isEmpty());
        assert(s.size() == 1);
        assert(s.contains(i1));
        assert(!s.contains(i2));
        for (Integer i : s) {
            assert(i == i1);
        }

        assert(s.add(i2) == true);
        assert(s.size() == 2);
        assert(s.contains(i1));
        assert(s.contains(i2));
        for (Integer i : s) {
            assert(i == i1 || i == i2);
        }

        assert(s.add(i2) == false);
        assert(s.size() == 2);

        assert(s.remove(i1) == true);
        assert(s.size() == 1);
        assert(!s.contains(i1));
        assert(s.contains(i2));
        for (Integer i : s) {
            assert(i == i2);
        }
    }
}
