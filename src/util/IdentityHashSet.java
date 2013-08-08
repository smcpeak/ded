// IdentityHashSet.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/** A set of objects, identified using reference equality,
  * like java.util.IdentityHashMap. */
public class IdentityHashSet<T> implements Set<T> {
    // --------------------- class data ----------------------
    /** The thing to which all the present elements are mapped in 'map'. */
    private static Object theValue = Integer.valueOf(0);

    // --------------------- instance data ----------------------
    /** The set is implemented using a map.  That is not very efficient,
      * but it will do. */
    private IdentityHashMap<T, Object> map = new IdentityHashMap<T, Object>();

    // --------------------- instance methods ----------------------
    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return this.map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return this.map.keySet().iterator();
    }

    @Override
    public Object[] toArray()
    {
        return this.map.keySet().toArray();
    }

    @Override
    public <T2> T2[] toArray(T2[] a)
    {
        return this.map.keySet().toArray(a);
    }

    @Override
    public boolean add(T e)
    {
        boolean already = contains(e);
        this.map.put(e, theValue);
        return !already;
    }

    @Override
    public boolean remove(Object o)
    {
        Object prevValue = this.map.remove(o);
        return prevValue != null;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return this.map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        boolean changed = false;
        for (T t : c) {
            changed = changed & this.add(t);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();      // just not implemented
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean changed = false;
        for (Object o : c) {
            changed = changed & this.remove(o);
        }
        return changed;
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }
}
