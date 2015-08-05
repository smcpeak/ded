// BDFParser.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.XParse;

/** Parse a set of font glyphs out of a BDF file:
  * https://partners.adobe.com/public/developer/en/font/5005.BDF_Spec.pdf
  *
  * For now, it only parses the attributes needed for rendering the
  * glyphs to a computer screen without scaling.  Additionally, I am
  * only trying to parse one particular file (helvR12sm.bdf) right
  * now, so the parser ignores features not used in that file. */
public class BDFParser {
    // ---- types ----
    public static class Glyph {
        /** STARTCHAR: Name of the character represented. */
        public String characterName;

        /** ENCODING: Unicode code point of the character represented
          * by this glyph. */
        public int codePoint;

        /** DWIDTH: Pixels to move the baseline point after drawing
          * this glyph. */
        public int dwx0;
        public int dwy0;

        /** BBX (part 1): Bounding box size in pixels. */
        public int bbw;
        public int bbh;

        /** BBX (part 2): Bounding box offset.  Add these pixel
          * offsets to the baseline point to get to the location where
          * the bounding box lower left corner should be placed. */
        public int bbxoff0x;

        /** Note: For this offset, positive values mean move *up*. */
        public int bbyoff0y;

        /** BITMAP: 2D array of bits.
          *
          * The outer array has one element per line, starting from the
          * top.  There are 'bbh' lines.
          *
          * The inner array has one integer in [0,255] per group of 8
          * pixels, starting from the left.  Within each integer, the
          * most significant bit is the leftmost column.  A bit of 1
          * means to draw the pixel.  There are 'bbw' defined columns;
          * the remainder are padded with zeroes. */
        public int bits[][];

        public String toString()
        {
            try {
                return toJSONObject().toString(2);
            }
            catch (JSONException e) {
                return "<Glyph.toString failed with exception: \""+e.getMessage()+"\">";
            }
        }

        public JSONObject toJSONObject() throws JSONException
        {
            JSONObject o = new JSONObject();

            o.put("characterName", this.characterName);
            o.put("codePoint", this.codePoint);
            o.put("dwx0", this.dwx0);
            o.put("dwy0", this.dwy0);
            o.put("bbw", this.bbw);
            o.put("bbh", this.bbh);
            o.put("bbxoff0x", this.bbxoff0x);
            o.put("bbxoff0y", this.bbyoff0y);

            JSONArray bitsArray = new JSONArray();
            for (int y=0; y < this.bits.length; y++) {
                JSONArray lineArray = new JSONArray();
                for (int x=0; x < this.bits[y].length; x++) {
                    lineArray.put(this.bits[y][x]);
                }
                bitsArray.put(lineArray);
            }
            o.put("bits", bitsArray);

            return o;
        }
    }

    // ---- data ----
    /** FONT: Name of the font, like "-Adobe-Helvetica-Medium-...". */
    public String fontName;

    /** SIZE: Point size and intended DPI. */
    public int pointSize;
    public int xresDPI;
    public int yresDPI;

    /** CAP_HEIGHT: Ascent. */
    public int ascent;

    /** CHARS: Sequence of glyphs. */
    public ArrayList<Glyph> glyphs = new ArrayList<Glyph>();

    // ---- methods ----
    /** Parse the BDF file contents from 'inputBytes'.  Throw XParse on
      * malformed input. */
    public BDFParser(InputStream inputBytes) throws Exception
    {
        parse(new InputStreamReader(inputBytes, "US-ASCII"));
    }

    /** Parse the BDF contents from 'inputCharacters'.  Throw XParse
      * on malformed input. */
    public BDFParser(Reader inputCharacters) throws Exception
    {
        parse(inputCharacters);
    }

    private void parse(Reader inputReader) throws Exception
    {
        BufferedReader in = new BufferedReader(inputReader);
        String line = in.readLine();
        int lineNum = 1;

        try {
            // Check the file header.  Beyond this, the parsing will be
            // quite loose.
            if (line == null) {
                throw new XParse("input is empty");
            }
            if (!line.startsWith("STARTFONT 2.")) {
                throw new XParse("first line must start with \"STARTFONT 2.\".");
            }

            while ((line = in.readLine()) != null) {
                lineNum++;
                String[] words = line.split(" ");
                if (words.length == 0) {
                    throw new XParse("blank line is invalid");
                }
                else if (words[0].equals("FONT") && words.length >= 2) {
                    this.fontName = words[1];
                }
                else if (words[0].equals("SIZE") && words.length >= 4) {
                    this.pointSize = Integer.valueOf(words[1]);
                    this.xresDPI = Integer.valueOf(words[2]);
                    this.yresDPI = Integer.valueOf(words[3]);
                }
                else if (words[0].equals("CAP_HEIGHT") && words.length >= 2) {
                    this.ascent = Integer.valueOf(words[1]);
                }
                else if (words[0].equals("STARTCHAR") && words.length >= 2) {
                    Glyph glyph = new Glyph();
                    glyph.characterName = words[1];

                    // Parse the glyph attributes.
                    while ((line = in.readLine()) != null) {
                        lineNum++;
                        words = line.split(" ");
                        if (words.length == 0) {
                            throw new XParse("blank line is invalid");
                        }
                        else if (words[0].equals("ENCODING") && words.length >= 2) {
                            glyph.codePoint = Integer.valueOf(words[1]);
                        }
                        else if (words[0].equals("DWIDTH") && words.length >= 3) {
                            glyph.dwx0 = Integer.valueOf(words[1]);
                            glyph.dwy0 = Integer.valueOf(words[2]);
                        }
                        else if (words[0].equals("BBX") && words.length >= 5) {
                            glyph.bbw = Integer.valueOf(words[1]);
                            glyph.bbh = Integer.valueOf(words[2]);
                            glyph.bbxoff0x = Integer.valueOf(words[3]);
                            glyph.bbyoff0y = Integer.valueOf(words[4]);
                        }
                        else if (words[0].equals("BITMAP")) {
                            if (glyph.bbw == 0 || glyph.bbh == 0) {
                                throw new XParse("glyph BITMAP without preceding BBX");
                            }
                            int widthBytes = (glyph.bbw + 7)/8;
                            glyph.bits = new int[glyph.bbh][widthBytes];

                            // Parse the bitmap lines.
                            int y = 0;
                            while ((line = in.readLine()) != null) {
                                lineNum++;

                                if (line.equals("ENDCHAR")) {
                                    if (y != glyph.bbh) {
                                        throw new XParse("glyph had "+y+
                                                         " BITMAP lines but BBX declared "+glyph.bbh);
                                    }
                                    break;
                                }

                                if (line.length() != widthBytes*2) {
                                    throw new XParse("glyph row has "+(line.length())+
                                                     " characters but should have "+widthBytes*2+
                                                     " according to BBX width "+glyph.bbw);
                                }

                                for (int b=0; b < widthBytes; b++) {
                                    char hi = line.charAt(b*2);
                                    char lo = line.charAt(b*2+1);
                                    int val = Integer.parseInt(""+hi+lo, 16);
                                    glyph.bits[y][b] = val;
                                }

                                y++;
                            } // while (bitmap lines)

                            if (line == null) {
                                throw new XParse("file ended without ENDCHAR for \""+
                                                 glyph.characterName+"\"");
                            }
                            else {
                                // ENDCHAR ends both the bitmap lines and the
                                // glyph attributes.
                                break;
                            }
                        }
                        else if (words[0].equals("ENDCHAR")) {
                            throw new XParse("ENDCHAR for \""+glyph.characterName+
                                             "\" appears before BITMAP");
                        }
                    } // while (glyph attributes)

                    if (line == null) {
                        throw new XParse("file ended without providing BITMAP for \""+
                            glyph.characterName+"\"");
                    }

                    this.glyphs.add(glyph);
                }
                else if (words[0].equals("ENDFONT")) {
                    // Normally this coincides with end of file, but
                    // I do not check that.  I just stop parsing at
                    // whichever occurs first.
                    break;
                }
            } // while (font attributes)
        }
        catch (XParse e) {
            // Re-throw with a bit more context.
            throw new XParse("BDFParser: line "+lineNum+": "+e.getMessage());
        }
    }

    public String toString()
    {
        try {
            return toJSONObject().toString(2);
        }
        catch (JSONException e) {
            return "<BDFParser.toString failed with exception: \""+e.getMessage()+"\">";
        }
    }

    public JSONObject toJSONObject() throws JSONException
    {
        JSONObject o = new JSONObject();

        o.put("fontName", this.fontName);
        o.put("pointSize", this.pointSize);
        o.put("xresDPI", this.xresDPI);
        o.put("yresDPI", this.yresDPI);
        o.put("ascent", this.ascent);

        JSONArray glyphsArray = new JSONArray();
        for (Glyph g : this.glyphs) {
            glyphsArray.put(g.toJSONObject());
        }
        o.put("glyphs", glyphsArray);

        return o;
    }

    // ---- test code ----
    public static void main(String args[]) throws Exception
    {
        String input =
            "STARTFONT 2.1\n"+
            "FONT -some-font-name\n"+
            "SIZE 12 75 75\n"+
            "STARTPROPERTIES 1\n"+
            "CAP_HEIGHT 9\n"+
            "ENDPROPERTIES\n"+
            "CHARS 123\n"+
            "STARTCHAR char0\n"+
            "ENCODING 0\n"+
            "SWIDTH 722 0\n"+
            "DWIDTH 9 0\n"+
            "BBX 7 9 1 0\n"+
            "BITMAP\n"+
            "AA\n"+
            "00\n"+
            "82\n"+
            "00\n"+
            "82\n"+
            "00\n"+
            "82\n"+
            "00\n"+
            "AA\n"+
            "ENDCHAR\n"+
            "STARTCHAR space\n"+
            "ENCODING 32\n"+
            "SWIDTH 278 0\n"+
            "DWIDTH 4 0\n"+
            "BBX 1 1 0 0\n"+
            "BITMAP\n"+
            "00\n"+
            "ENDCHAR\n"+
            "STARTCHAR exclam\n"+
            "ENCODING 33\n"+
            "SWIDTH 278 0\n"+
            "DWIDTH 3 0\n"+
            "BBX 1 9 1 0\n"+
            "BITMAP\n"+
            "80\n"+
            "80\n"+
            "80\n"+
            "80\n"+
            "80\n"+
            "80\n"+
            "80\n"+
            "00\n"+
            "80\n"+
            "ENDCHAR\n"+
            "STARTCHAR numbersign\n"+
            "ENCODING 35\n"+
            "SWIDTH 556 0\n"+
            "DWIDTH 7 0\n"+
            "BBX 6 8 0 0\n"+
            "BITMAP\n"+
            "28\n"+
            "28\n"+
            "FC\n"+
            "28\n"+
            "FC\n"+
            "50\n"+
            "50\n"+
            "50\n"+
            "ENDCHAR\n"+
            "ENDFONT\n"+
            "";

        String expected =
            "{\n"+
            "  \"ascent\": 9,\n"+
            "  \"fontName\": \"-some-font-name\",\n"+
            "  \"glyphs\": [\n"+
            "    {\n"+
            "      \"bbh\": 9,\n"+
            "      \"bbw\": 7,\n"+
            "      \"bbxoff0x\": 1,\n"+
            "      \"bbxoff0y\": 0,\n"+
            "      \"bits\": [\n"+
            "        [170],\n"+
            "        [0],\n"+
            "        [130],\n"+
            "        [0],\n"+
            "        [130],\n"+
            "        [0],\n"+
            "        [130],\n"+
            "        [0],\n"+
            "        [170]\n"+
            "      ],\n"+
            "      \"characterName\": \"char0\",\n"+
            "      \"codePoint\": 0,\n"+
            "      \"dwx0\": 9,\n"+
            "      \"dwy0\": 0\n"+
            "    },\n"+
            "    {\n"+
            "      \"bbh\": 1,\n"+
            "      \"bbw\": 1,\n"+
            "      \"bbxoff0x\": 0,\n"+
            "      \"bbxoff0y\": 0,\n"+
            "      \"bits\": [[0]],\n"+
            "      \"characterName\": \"space\",\n"+
            "      \"codePoint\": 32,\n"+
            "      \"dwx0\": 4,\n"+
            "      \"dwy0\": 0\n"+
            "    },\n"+
            "    {\n"+
            "      \"bbh\": 9,\n"+
            "      \"bbw\": 1,\n"+
            "      \"bbxoff0x\": 1,\n"+
            "      \"bbxoff0y\": 0,\n"+
            "      \"bits\": [\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [128],\n"+
            "        [0],\n"+
            "        [128]\n"+
            "      ],\n"+
            "      \"characterName\": \"exclam\",\n"+
            "      \"codePoint\": 33,\n"+
            "      \"dwx0\": 3,\n"+
            "      \"dwy0\": 0\n"+
            "    },\n"+
            "    {\n"+
            "      \"bbh\": 8,\n"+
            "      \"bbw\": 6,\n"+
            "      \"bbxoff0x\": 0,\n"+
            "      \"bbxoff0y\": 0,\n"+
            "      \"bits\": [\n"+
            "        [40],\n"+
            "        [40],\n"+
            "        [252],\n"+
            "        [40],\n"+
            "        [252],\n"+
            "        [80],\n"+
            "        [80],\n"+
            "        [80]\n"+
            "      ],\n"+
            "      \"characterName\": \"numbersign\",\n"+
            "      \"codePoint\": 35,\n"+
            "      \"dwx0\": 7,\n"+
            "      \"dwy0\": 0\n"+
            "    }\n"+
            "  ],\n"+
            "  \"pointSize\": 12,\n"+
            "  \"xresDPI\": 75,\n"+
            "  \"yresDPI\": 75\n"+
            "}\n"+
            "";

        StringReader reader = new StringReader(input);
        BDFParser parser = new BDFParser(reader);
        String output = parser.toString()+"\n";

        if (output.equals(expected)) {
            System.out.println("BDFParser: tests pass");
        }
        else {
            System.out.println("Expected output:");
            System.out.print(expected);
            System.out.println("Actual output:");
            System.out.print(output);
            System.out.println("BDFParser: tests FAIL");
            System.exit(2);
        }
    }
}

// EOF
