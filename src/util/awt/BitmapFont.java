// BitmapFont.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.util.HashMap;

/** Class to store and render a bitmap-based font.
  *
  * This class is an alternative (with different API) to java.awt.Font,
  * which has proved very non-portable, both among different OSes
  * and among different JVMs (so frustrating!).
  *
  * The drawing performed by this class is significantly slower than
  * when using AWT Font.  For tests/test.ded, drawing with AWT Font
  * gets ~500 FPS, while using this class gets ~400 FPS.  However,
  * since drawing text is only part of the work for each frame, that
  * means the text drawing is more than 20% slower.  I have not done
  * careful measurements but I'd estimate just drawing text is 2-3x slower
  * with this class.  But, as my editor is still getting good frame
  * rates, for now I'm not spending more time trying to optimize it. */
public class BitmapFont {
    // ---- types ---
    /** Class to carry multiple values out of the render routine. */
    private static class RenderMetrics {
        /** Bounding rectangle for the rendered text. */
        public Rectangle bound;

        /** Sum of all the character offsets to next character. */
        public Point offsetSum;
    }

    // ---- data ----
    /** The parsed font metrics and glyphs. */
    private BDFParser bdfParser;

    /** Map from code point to BDF glyph. */
    private HashMap<Integer, BDFParser.Glyph> codeToBitmapGlyph =
        new HashMap<Integer, BDFParser.Glyph>();

    /** Map from Color to code point to colored glyph image. */
    private HashMap<Color, HashMap<Integer, BufferedImage>> colorToCodeToGlyph =
        new HashMap<Color, HashMap<Integer, BufferedImage>>();

    // ---- methods ----
    public BitmapFont(BDFParser bdf)
    {
        this.bdfParser = bdf;

        // Build the bitmap glyph map.
        for (BDFParser.Glyph g : bdf.glyphs) {
            this.codeToBitmapGlyph.put(g.codePoint, g);
        }
    }

    /** Draw 'str' into 'g' with the baseline at 'y' and start of the
      * first character at 'x'. */
    public void drawString(Graphics g, String str, int x, int y)
    {
        drawOrMeasureString(g, str, x, y);
    }

    /** Core render routine that can both draw the text as well as
      * measure what it would do.  If 'g' is null, we just measure. */
    public RenderMetrics drawOrMeasureString(Graphics g, String str, int x0, int y0)
    {
        Color color = g==null? null : g.getColor();

        Rectangle bound = null;
        int x = x0;
        int y = y0;

        for (int i=0; i < str.length(); i++) {
            int codePoint = str.codePointAt(i);
            if (codePoint > 0xFFFF) {
                i++;    // Skip the second half of the surrogate pair.
            }

            // Find the BDF definition of this character.
            BDFParser.Glyph bitmap = this.codeToBitmapGlyph.get(codePoint);
            if (bitmap == null) {
                // Assume that code point 0 is suitable as a replacement character.
                codePoint = 0;
                bitmap = this.codeToBitmapGlyph.get(codePoint);
                if (bitmap == null) {
                    // Just skip it then.
                    continue;
                }
            }

            // Compute the bound for this character.
            Rectangle r = new Rectangle(x + bitmap.bbxoff0x,
                                        y - bitmap.bbyoff0y - bitmap.bbh,
                                        bitmap.bbw, bitmap.bbh);

            // Combine it with the bound so far.
            if (bound == null) {
                bound = r;
            }
            else {
                bound.add(r);
            }

            if (g != null) {
                // Get the image for that character in the chosen color.
                BufferedImage glyphImage = getGlyphImage(color, codePoint, bitmap);

                // Draw it to 'g'.
                g.drawImage(glyphImage, r.x, r.y, null /*obs*/);
            }

            // Move past this character.  There is no kerning.
            x += bitmap.dwx0;
            y += bitmap.dwy0;

            // For reasons I do not understand, the Oracle JVM renders
            // the space character with 3 pixels of space instead of the
            // 4 it shows in the BDF file.  For the moment, I'm going to
            // try to replicate that behavior in code rather than change
            // the font definition.
            if (codePoint == 32) {
                x--;
            }
        }

        RenderMetrics rm = new RenderMetrics();
        rm.bound = bound;
        rm.offsetSum = new Point(x-x0, y-y0);
        return rm;
    }

    /** Given a color and code point, look up or create an image
      * containing the bitmap in 'bitmap' suitable for drawing. */
    private BufferedImage getGlyphImage(Color color, int codePoint, BDFParser.Glyph bitmap)
    {
        // Get or create the color-dependent map.
        HashMap<Integer, BufferedImage> codeToImage =
            this.colorToCodeToGlyph.get(color);
        if (codeToImage == null) {
            codeToImage = new HashMap<Integer, BufferedImage>();
            this.colorToCodeToGlyph.put(color, codeToImage);
        }

        // Check the color-dependent map for this code point.
        BufferedImage bi = codeToImage.get(codePoint);
        if (bi != null) {
            return bi;
        }

        // It appears that this automatically initializes to the entire
        // image being transparent initially.
        //
        // I'm not sure about TYPE_INT_ARGB here.  I could imagine that
        // it would be more efficient to use a different type, depending
        // on the graphics hardware.
        bi = new BufferedImage(bitmap.bbw, bitmap.bbh,
                               BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        g.setColor(color);

        // This is not particularly efficient, but it does not need to
        // be since we only render each glyph once per color.
        for (int y=0; y < bitmap.bits.length; y++) {
            for (int b=0; b < bitmap.bits[y].length; b++) {
                int n = bitmap.bits[y][b];
                for (int bit=0; bit < 8 && (b*8 + bit) < bitmap.bbw; bit++, n <<= 1) {
                    if ((n & 0x80) != 0) {
                        // This draws a single pixel.
                        g.drawLine(b*8 + bit, y,
                                   b*8 + bit, y);
                    }
                }
            }
        }

        // Ensure any pending operations are flushed.
        g.dispose();

        codeToImage.put(codePoint, bi);
        return bi;
    }

    /** Return the distance from the starting baseline to the ending
      * baseline point when string 'str' is rendered.  This is like
      * java.awt.FontMetrics.stringWidth; and as such, it is different
      * from stringBound().height because the latter takes account of
      * the bounding boxes of the first and last characters (not just
      * the inter-character distance). */
    public int stringWidth(String str)
    {
        RenderMetrics rm = drawOrMeasureString(null, str, 0,0);
        return rm.offsetSum.x;
    }

    /** Return a bounding rectangle for the pixels rendered for 'str',
      * where (0,0) would be the start baseline, i.e., the argument
      * to 'drawString'.  Returns (0,0,0,0) if 'str' is empty. */
    public Rectangle stringBound(String str)
    {
        RenderMetrics rm = drawOrMeasureString(null, str, 0,0);
        return rm.bound;
    }

    /** Return the usual height of a line of text.  This is meant to
      * have the same semantics as java.awt.FontMetrics.getHeight. */
    public int getStandardLineHeight()
    {
        // For the example font I have, when it is converted to TTF,
        // then rendered with the Oracle JVM, FontMetrics.getHeight
        // says 14.  I do not know where that number comes from, but
        // I want to match it (both for compatibility and aesthetics).
        // So I'm just going to make a guess, that is likely wrong, of
        // how to compute it.
        return this.bdfParser.pointSize + 2;
    }

    /** Distance from baseline to the top of capital letters, like
      * java.awt.font.LineMetrics.getAscent. */
    public int getAscent()
    {
        return this.bdfParser.ascent;
    }

    /** Like java.awt.FontMetrics.getMaxAscent.  That function's docs
      * say "no character extends farther above the baseline", but for
      * my sample font, Oracle JVM says 10 but there *are* characters
      * that go higher, so I'm not sure what the right logic is. */
    public int getMaxAscent()
    {
        return this.getAscent() + 1;       // Probably not right.
    }

    /** Distance from baseline to bottom of descenders, like
      * java.awt.font.LineMetrics.getDescent. */
    public int getDescent()
    {
        return this.bdfParser.pointSize - getAscent();
    }

    /** Distance to underline, like
      * java.awt.font.LineMetrics.getUnderlineOffset. */
    public int getUnderlineOffset()
    {
        // No idea where this comes from.  It also seems like this
        // should be 2 for the font I am using, but Java says 1,
        // so I use that and keep my workaround in the caller code.
        return 1;
    }

    /** Draw 'str' centered at 'p'. */
    public void drawCenteredText(Graphics g, Point p, String str)
    {
        // Go to 'p', then add a/2 to get to the baseline.
        // I ignore the descent because it looks better to center without
        // regard to descenders.
        int baseY = p.y + getAscent()/2;
        int baseX = p.x - stringWidth(str)/2;

        this.drawString(g, str, baseX, baseY);
    }

    /** Draw 'str' at the given location, but process newlines by moving
      * to a new line. */
    public void drawTextWithNewlines(Graphics g, String str, int x, int y)
    {
        String lines[] = str.split("\n");
        int lineHeight = this.getStandardLineHeight();
        for (String s : lines) {
            this.drawString(g, s, x, y);
            y += lineHeight;
        }
    }
}

// EOF
