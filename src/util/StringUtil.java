// StringUtil.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** General-purpose string utilities. */
public class StringUtil {
    /** Return 's' as a quoted JSON syntax using only ASCII characters. */
    public static String quoteAsJSONASCII(String s)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i=0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\'':
                    sb.append("\\'");
                    break;

                case '\\':
                    sb.append("\\\\");
                    break;

                case '\b':
                    sb.append("\\b");
                    break;

                case '\f':
                    sb.append("\\f");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;

                case '\r':
                    sb.append("\\r");
                    break;

                case '\t':
                    sb.append("\\t");
                    break;

                default:
                    if (32 <= c && c <= 126) {
                        sb.append(c);
                    }
                    else {
                        sb.append(String.format("\\u%04X", (int)c));
                    }
                    break;
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /** Attempt to match a substring of 'stringToParse' against 'regex'.
      * If a match is found, return an array of all the capture groups,
      * where element 0 is the entire matched substring, element 1 is
      * the first capture group, etc.  Otherwise, return null. */
    public static String[] parseByRegex(String stringToParse, String regex)
    {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(stringToParse);
        if (m.find()) {
            int n = m.groupCount()+1;
            String[] ret = new String[n];
            for (int i=0; i < n; i++) {
                ret[i] = m.group(i);
            }
            return ret;
        }
        else {
            return null;
        }
    }
}

// EOF
