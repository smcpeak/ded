// StringUtilTests.java
// See toplevel license.txt for copyright and license terms.

package util;

import util.StringUtil;


/** Tests for 'StringUtil'. */
class StringUtilTests {
    public static void main(String[] args)
    {
        StringUtilTests t = new StringUtilTests();
        t.testJoinAdjacentLines();
    }

    private void tjal1(
        String input,
        String expect)
    {
        String actual = StringUtil.joinAdjacentLines(input);
        if (!actual.equals(expect)) {
            System.out.println("input : "+input);
            System.out.println("expect: "+expect);
            System.out.println("actual: "+actual);
            System.exit(2);
        }
    }

    private void testJoinAdjacentLines()
    {
        tjal1("", "");
        tjal1("x", "x");
        tjal1("x\n", "x\n");
        tjal1("x\ny", "x y");
        tjal1("x \ny", "x y");
        tjal1("x  \ny", "x  y");
        tjal1("x\t\ny", "x\ty");
        tjal1("one two\nthree four\nfive\n\nsix seven\neight",
              "one two three four five\nsix seven eight");
        tjal1("x\n\ny", "x\ny");
        tjal1("x\n\n\ny", "x\n\ny");
        tjal1("Hi.\nHello.", "Hi.  Hello.");
        tjal1("Hi!\nHello.", "Hi!  Hello.");
        tjal1("Hi?\nHello.", "Hi?  Hello.");
        tjal1("Hi;\nhello.", "Hi; hello.");
        tjal1("a\n\nb\n\nc\n\nd\n",
              "a\nb\nc\nd\n");
        tjal1("a\n\nb\n\nc\n\nd\n\n",
              "a\nb\nc\nd\n\n");

        tjal1("something\n"+
              "\n"+
              "```\n"+
              "  literal\n"+
              "    section\n"+
              "```\n"+
              "\n"+
              "else\n",

              "something\n"+
              "\n"+
              "  literal\n"+
              "    section\n"+
              "\n"+
              "else\n");

        tjal1("something\n"+
              "```\n"+
              "  literal\n"+
              "    section\n"+
              "```\n"+
              "else\n",

              "something\n"+
              "  literal\n"+
              "    section\n"+
              "else\n");
    }
}


// EOF
