// IntRange.java

package util;

/** Represents a contiguous range of integer values. */
public class IntRange {
    // --------------------- instance data ---------------------
    /* Designated range, inclusive.
     * 
     * Invariant: low <= high. */
    public int low, high; 
    
    // ------------------------ methods ------------------------
    public IntRange(int low, int high)
    {
        assert(low <= high);
        this.low = low;
        this.high = high;
    }
    
    /** Yield a range that covers just the given integer.
      *
      * This is not a constructor because it seems like it should have
      * more explicit syntax. */
    public static IntRange singleton(int v)
    {
        return new IntRange(v, v);
    }
    
    public boolean contains(int v)
    {
        return this.low <= v && v <= this.high;
    }
    
    public int midPoint()
    {
        return Util.avg(this.low, this.high);
    }
}

// EOF
