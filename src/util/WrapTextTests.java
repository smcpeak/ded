// WrapTextTests.java
// See toplevel license.txt for copyright and license terms.

package util;


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
        // For ease of testing, just say that every character is one
        // pixel.
        return end-start;
    }

    private void testWrapLine(
        WrapTextPolicy policy,
        int maxWidth,
        String origLine,
        String expect)
    {
        String actual =
            WrapText.wrapLine(policy, maxWidth, origLine, this);
        if (!actual.equals(expect)) {
            System.out.println(
                "testWrapLine failed:\n"+
                "  policy: " + policy.toString() + "\n" +
                "  origLine: " + StringUtil.doubleQuote(origLine) + "\n" +
                "  maxWidth: " + maxWidth + "\n" +
                "  expect: " + StringUtil.doubleQuote(expect) + "\n" +
                "  actual: " + StringUtil.doubleQuote(actual));
            System.exit(2);
        }
    }

    private void testNoWrap()
    {
        testWrapLine(
            WrapTextPolicy.WTP_NoWrap,
            5,
            "",
            "");

        testWrapLine(
            WrapTextPolicy.WTP_NoWrap,
            5,
            "a long line that would wrap normally",
            "a long line that would wrap normally");
    }

    private void testWSWrap(
        int maxWidth,
        String origLine,
        String expect)
    {
        testWrapLine(WrapTextPolicy.WTP_Whitespace, maxWidth,
                     origLine, expect);
    }

    private void testWhitespaceWrap()
    {
        testWSWrap(10, "", "");
        testWSWrap(10, "\n", "\n");
        testWSWrap(10, "\n\n", "\n\n");

        testWSWrap(10,
            "a long line that can wrap a few times",
            "a long\n"+
            "line that\n"+
            "can wrap a\n"+
            "few times");

        testWSWrap(10,
            "a long line with_a_long_string_inside that can wrap a few times",
            "a long\n"+
            "line\n"+
            "with_a_long_string_inside\n"+
            "that can\n"+
            "wrap a few\n"+
            "times");

        testWSWrap(100,
            "a long line that can wrap a few times",
            "a long line that can wrap a few times");
    }

    private void testSWrap(
        int maxWidth,
        String origLine,
        String expect)
    {
        testWrapLine(WrapTextPolicy.WTP_Sentence, maxWidth,
                     origLine, expect);
    }

    private void testSentenceWrap()
    {
        testSWrap(10, "", "");
        testSWrap(10, "\n", "\n");
        testSWrap(10, "\n\n", "\n\n");

        testSWrap(10,
            "This is a sentence.  This is another: sort of.  What?  x",
            "This is a sentence.\nThis is another:\nsort of.\nWhat?  x");
    }
}


// EOF
