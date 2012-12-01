// FlattenInputStream.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

/** Stream for reading files created by smbase/flatten. */
public class FlattenInputStream extends BufferedInputStream {
    // -------------------- instance data ----------------------
    /** Version of the format being read.  Initially 0, it is up
      * to the calling code to read and write this as it sees fit. */
    public int version;

    /** Map from integer reference code to the associated Object.
      * This is for 'noteOwner' and 'readSerf'. */
    protected ArrayList<Object> intToOwner;

    // ----------------------- methods -------------------------
    public FlattenInputStream(InputStream in)
    {
        super(in);
        this.version = 0;
        this.intToOwner = new ArrayList<Object>();
        this.intToOwner.add(null);      // dummy entry b/c we start at 1
    }

    /** Read the next integer and check that it is 'expected'.  Throw
      * XParse if it is not. */
    public void checkpoint(int expected)
        throws XParse, IOException
    {
        int actual = this.readInt();
        if (actual != expected) {
            throw new XParse(
                String.format("checkpoint: expected 0x%08X but found 0x%08X",
                              expected, actual));
        }
    }

    /** Read a 32-bit signed integer stored in little-endian format. */
    public int readInt()
        throws XParse, IOException
    {
        byte[] b = new byte[4];
        this.readBytes(b);
        return ((b[0] & 0xff) <<  0) |
               ((b[1] & 0xff) <<  8) |
               ((b[2] & 0xff) << 16) |
               ((b[3] & 0xff) << 24);
    }

    /** Read bytes to fill 'buf', blocking if needed, or throw IOException. */
    public void readBytes(byte[] buf)
        throws XParse, IOException
    {
        int offset = 0;
        while (offset < buf.length) {
            int len = this.read(buf, offset, buf.length - offset);
            if (len < 0) {
                throw new XParse("unexpected EOF while reading "+buf.length+" bytes");
            }
            offset += len;
        }
        assert(offset == buf.length);
    }

    /** Read a Dimension, stored as width then height. */
    public Dimension readDimension()
        throws XParse, IOException
    {
        int w = this.readInt();
        int h = this.readInt();
        return new Dimension(w, h);
    }

    /** Read a Point, stored as x then y. */
    public Point readPoint()
        throws XParse, IOException
    {
        int x = this.readInt();
        int y = this.readInt();
        return new Point(x, y);
    }

    /** Read a String, stored as a length-prefixed sequence of ASCII bytes. */
    public String readString()
        throws XParse, IOException
    {
        int len = this.readInt();
        if (len == -1) {
            return null;
        }

        byte[] buf = new byte[len+1];    // Final byte is 0.
        this.readBytes(buf);

        if (buf[len] != 0) {
            throw new XParse("string does not end with 0 byte");
        }

        // Do not pass the 0 byte to the String constructor.
        return new String(buf, 0, len, Charset.forName("US-ASCII"));
    }

    /** Record an owned object so it can be referred to later. */
    public void noteOwner(Object ownerPtr)
    {
        assert(ownerPtr != null);
        this.intToOwner.add(ownerPtr);
    }

    /** Read a serf object reference, consult the owner table, and
      * return the associated object.  This can return null. */
    public Object readSerf()
        throws XParse, IOException
    {
        int index = this.readInt();
        if (index == 0) {
            return null;
        }
        if (!( 1 <= index && index < this.intToOwner.size() )) {
            throw new XParse("invalid serf ref "+index+"; should be in [1,"+
                             this.intToOwner.size()+"]");
        }
        Object ret = this.intToOwner.get(index);
        assert(ret != null);
        return ret;
    }

    /** Read a boolean. */
    public boolean readBoolean()
        throws XParse, IOException
    {
        // I think/hope 'bool' is 1 byte on all the machines where
        // I created .er files...
        byte[] b = new byte[1];
        this.readBytes(b);
        return b[0] != 0;
    }
}

// EOF
