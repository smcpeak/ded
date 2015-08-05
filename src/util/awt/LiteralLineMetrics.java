// LiteralLineMetrics.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.font.LineMetrics;

/** Implement LineMetrics with a bunch of public fields. */
public class LiteralLineMetrics extends LineMetrics {
    // ---- data ----
    public float ascent;
    public int baselineIndex;
    public float[] baselineOffsets = new float[3];
    public float descent;
    public float height;
    public float leading;
    public int numChars;
    public float strikethroughOffset;
    public float strikethroughThickness;
    public float underlineOffset;
    public float underlineThickness;

    @Override
    public int getNumChars() {
        return this.numChars;
    }

    @Override
    public float getAscent() {
        return this.ascent;
    }

    @Override
    public float getDescent() {
        return this.descent;
    }

    @Override
    public float getLeading() {
        return this.leading;
    }

    @Override
    public float getHeight() {
        return this.height;
    }

    @Override
    public int getBaselineIndex() {
        return this.baselineIndex;
    }

    @Override
    public float[] getBaselineOffsets() {
        return this.baselineOffsets;
    }

    @Override
    public float getStrikethroughOffset() {
        return this.strikethroughOffset;
    }

    @Override
    public float getStrikethroughThickness() {
        return this.strikethroughThickness;
    }

    @Override
    public float getUnderlineOffset() {
        return this.underlineOffset;
    }

    @Override
    public float getUnderlineThickness() {
        return this.underlineThickness;
    }
}

// EOF
