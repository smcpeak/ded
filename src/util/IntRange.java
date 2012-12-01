// IntRange.java
// See toplevel license.txt for copyright and license terms.

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

    /** Return the endpoint closer to 'p'.  Returns 'low' if
      * they are equidistant. */
    public int closerEnd(int p)
    {
        if (p <= this.low) {
            return this.low;
        }
        else if (p >= this.high) {
            return this.high;
        }
        else {
            int dlow = p - this.low;
            int dhigh = this.high - p;
            if (dlow <= dhigh) {
                return this.low;
            }
            else {
                return this.high;
            }
        }
    }
}

// EOF
