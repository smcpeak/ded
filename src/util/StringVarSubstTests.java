// StringVarSubstTests.java
// See toplevel license.txt for copyright and license terms.

package util;

/** Tests for 'StringVarSubst'. */
public class StringVarSubstTests {
    /** Run the unit tests. */
    public static void main(String[] args)
    {
        StringVarSubstTests t = new StringVarSubstTests();

        t.testBasics();
    }

    /** Simple replacer for testing. */
    private static class TestReplacer implements StringVarSubst.Replacer {
        public String getVar(String varName)
        {
            switch (varName) {
                case "": return "empty";
                case "x": return "ecks";
                case "123": return "onetwothree";
                case "hasnl": return "a\nb\nc\n";
                default: return "<undef: "+varName+">";
            }
        }
    }

    /** Test the basic functionality. */
    private void testBasics()
    {
        TestReplacer r = new TestReplacer();

        t1(r, "", "");
        t1(r, "$(", "$(");
        t1(r, "$(abc", "$(abc");
        t1(r, "$(abc)", "<undef: abc>");
        t1(r, "a $() b $(x) c $(123) d", "a empty b ecks c onetwothree d");
        t1(r, "$($($()))", "<undef: $($(>))");
        t1(r, "$(hasnl)", "a\nb\nc\n");
    }

    /** Run one test. */
    private void t1(StringVarSubst.Replacer r, String input, String expect)
    {
        String actual = StringVarSubst.substituteVariables(input, r);
        if (!actual.equals(expect)) {
            System.out.println("StringVarSubstTest failure:");
            System.out.println("  input : "+input);
            System.out.println("  expect: "+expect);
            System.out.println("  actual: "+actual);
            System.exit(2);
        }
    }
}

// EOF
