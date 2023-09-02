// WrapTextTests.java
// See toplevel license.txt for copyright and license terms.

package util;

import util.WrapText;


/** Tests for 'WrapText'. */
public class WrapTextTests implements StringMeasurer {
    public static void main(String[] args)
    {
        WrapTextTests t = new WrapTextTests();
        t.testNoWrap();
        t.testWhitespaceWrap();
        t.testSentenceWrap();
    }

    @Override
    public int substringWidth(String s, int start, int end)
    {
        // For ease of testing, just say that every character is ten
        // pixels.  (Testing with just one pixel per character would
        // risk failing to uncover a hypothetical bug that mixed up
        // string lengths and their pixel widths.)
        return (end-start) * 10;
    }

    /** Perform a single low-level test and check the output. */
    private static void testWrapLine(
        WrapTextPolicy policy,
        int maxWidth,
        int indentSpaces,
        String origLine,
        String expect,
        StringMeasurer measurer)
    {
        String actual = WrapText.wrapLine(policy, maxWidth,
            indentSpaces, origLine, measurer);

        if (!actual.equals(expect)) {
            System.out.println(
                "testWrapLine failed:\n"+
                "  policy: " + policy.toString() + "\n" +
                "  maxWidth: " + maxWidth + "\n" +
                "  indentSpaces: " + indentSpaces + "\n" +
                "  origLine: " + StringUtil.doubleQuote(origLine) + "\n" +
                "  expect: " + StringUtil.doubleQuote(expect) + "\n" +
                "  actual: " + StringUtil.doubleQuote(actual));
            System.exit(2);
        }
    }

    private void testNoWrap()
    {
        testWrapLine(
            WrapTextPolicy.WTP_NoWrap, 5, 0,
            "",
            "",
            this);

        testWrapLine(
            WrapTextPolicy.WTP_NoWrap, 5, 0,
            "a long line that would wrap normally",
            "a long line that would wrap normally",
            this);
    }

    private void testWSWrap(
        int maxWidth,
        int indentSpaces,
        String origLine,
        String expect)
    {
        testWrapLine(WrapTextPolicy.WTP_Whitespace, maxWidth,
                     indentSpaces, origLine, expect, this);
    }

    private void testWhitespaceWrap()
    {
        testWSWrap(100, 0, "", "");
        testWSWrap(100, 0, "\n", "\n");
        testWSWrap(100, 0, "\n\n", "\n\n");

        testWSWrap(100, 2, "", "");
        testWSWrap(100, 2, "\n", "\n");
        testWSWrap(100, 2, "\n\n", "\n\n");

        testWSWrap(100, 0,
            "a long line that can wrap a few times",
        //  [          ]
            "a long\n"+
            "line that\n"+
            "can wrap a\n"+
            "few times");

        testWSWrap(100, 2,
            "a long line that can wrap a few times",
        //  [          ]
            "a long\n"+
            "  line\n"+
            "  that can\n"+
            "  wrap a\n"+
            "  few\n"+
            "  times");

        // This has enough leading text to completely fill the first
        // line, and would overfill it if the algorithm erroneously
        // counted the indentation width against that first line.
        testWSWrap(100, 2,
            "aaaaa long line that can wrap a few times",
        //  [          ]
            "aaaaa long\n"+
            "  line\n"+
            "  that can\n"+
            "  wrap a\n"+
            "  few\n"+
            "  times");

        testWSWrap(100, 0,
            "a long line with_a_long_string_inside that can wrap a few times",
        //  [          ]
            "a long\n"+
            "line\n"+
            "with_a_long_string_inside\n"+
            "that can\n"+
            "wrap a few\n"+
            "times");

        testWSWrap(100, 2,
            "a long line with_a_long_string_inside that can wrap a few times",
        //  [          ]
            "a long\n"+
            "  line\n"+
            "  with_a_long_string_inside\n"+
            "  that can\n"+
            "  wrap a\n"+
            "  few\n"+
            "  times");

        testWSWrap(1000, 0,
            "a long line that can wrap a few times",
            "a long line that can wrap a few times");

        testWSWrap(1000, 2,
            "a long line that can wrap a few times",
            "a long line that can wrap a few times");
    }

    private void testSWrap(
        int maxWidth,
        int indentSpaces,
        String origLine,
        String expect)
    {
        testWrapLine(WrapTextPolicy.WTP_Sentence, maxWidth,
                     indentSpaces, origLine, expect, this);
    }

    private void testSentenceWrap()
    {
        testSWrap(100, 0, "", "");
        testSWrap(100, 0, "\n", "\n");
        testSWrap(100, 0, "\n\n", "\n\n");

        testSWrap(100, 2, "", "");
        testSWrap(100, 2, "\n", "\n");
        testSWrap(100, 2, "\n\n", "\n\n");

        testSWrap(100, 0,
            "This is a sentence.  This is another: sort of.  What?  x",
            "This is a sentence.\nThis is another:\nsort of.\nWhat?  x");

        testSWrap(100, 2,
            "This is a sentence.  This is another: sort of.  What?  x",
            "This is a sentence.\n  This is another:\n  sort of.\n  What?  x");

        testSWrap(100, 2,
            "This is a sentence.  This is another: sort of.  What?  xy",
            "This is a sentence.\n  This is another:\n  sort of.\n  What?\n  xy");
    }
}


// EOF
