// StringVarSubst.java
// See toplevel license.txt for copyright and license terms.

package util;

/** Class with a static method to perform variable substitutions using
  * the "$(...)" syntax. */
public class StringVarSubst {
    // ---- types ----
    /** Interface to return a string value for a string variable name. */
    public static interface Replacer {
        public String getVar(String varName);
    }

    // ---- methods ----
    /** Replace all occurrences of "$(...)" with whatever the replacer
      * says is the replacement of "...". */
    public static String substituteVariables(String src, Replacer replacer)
    {
        StringBuilder sb = new StringBuilder();

        // Next code unit index to process.
        int i = 0;

    outerLoop:
        while (i < src.length()) {
            if (i+1 < src.length() &&
                src.charAt(i) == '$' &&
                src.charAt(i+1) == '(')
            {
                // Find the close-paren.
                int j = i+2;
                while (j < src.length()) {
                    if (src.charAt(j) == ')') {
                        sb.append(replacer.getVar(src.substring(i+2, j)));

                        // Continue parsing right after the ')'.
                        i = j+1;
                        continue outerLoop;
                    }

                    ++j;
                }

                // Did not find ')', so just take all of the text
                // literally.
                sb.append(src.substring(i, j));
                break;
            }

            // Not the start of a variable reference, so accumulate the
            // character literally.
            sb.append(src.charAt(i));
            ++i;
        }

        return sb.toString();
    }
}

// EOF
