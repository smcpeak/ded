// StringUtil.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.Collection;

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

    /** Return 's' in Java double-quoted string literal syntax. */
    public static String doubleQuote(String s)
    {
        // This will suffice as an approximation for now.
        return quoteAsJSONASCII(s);
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

    /** Like String.format, except the format string, which is passed in
      * as English, will first be translated into the user's natural
      * language if a mapping is available. */
    public static String fmt(String formatString, Object... args)
    {
        // For now, there is no facility for actually doing a translation.
        // I just want this method so I can write code as if there was.
        return String.format(formatString, args);
    }

    /** Directly translate the English argument string to the user's
      * natural language.  This is different from fmt(english) because
      * all characters in 'english' are treated literally. */
    public static String localize(String english)
    {
        // For now, there is no actual translation.
        return english;
    }

    /** Return 's' repeated 'n' times. */
    public static String repeatString(String s, int n)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /** Return a string with n*2 spaces. */
    public static String indent(int n)
    {
        return repeatString("  ", n);
    }

    /** Parse 'input' as a 32-bit signed decimal integer.  Throw a
      * RuntimeException with a useful error message if its syntax
      * does not conform.  (This is as opposed to NumberFormatException,
      * which has no useful message.) */
    public static int parseInteger(String input)
    {
        try {
            return Integer.parseInt(input);
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("Malformed integer: \"" + input + "\".");
        }
    }

    /** Parse 'input' as a signed 32-bit decimal integer.  But if it is
      * empty, return null. */
    public static Integer parseOptionalInteger(String input)
    {
        if (input.isEmpty()) {
            return null;
        }
        return parseInteger(input);
    }

    /** Parse 'input' as a non-negative 32-bit decimal integer. */
    public static int parseNonnegativeInteger(String input)
    {
        int ret = parseInteger(input);
        if (ret < 0) {
            throw new RuntimeException("Non-negative value required, but "+
                                       ret+" was supplied.");
        }
        return ret;
    }

    /** Parse 'input' as a non-negative 32-bit decimal integer, or null if empty. */
    public static Integer parseOptionalNonnegativeInteger(String input)
    {
        Integer ret = parseOptionalInteger(input);
        if (ret != null && ret < 0) {
            throw new RuntimeException("Non-negative value required, but "+
                                       ret+" was supplied.");
        }
        return ret;
    }

    /** Parse 'input' as a positive 32-bit decimal integer, or null if empty. */
    public static Integer parseOptionalPositiveInteger(String input)
    {
        Integer ret = parseOptionalInteger(input);
        if (ret != null && ret <= 0) {
            throw new RuntimeException("Positive value required, but "+
                                       ret+" was supplied.");
        }
        return ret;
    }

    /** Given an input that is written like a text file with hard line
      * breaks within a paragraph and consecutive newlines between
      * paragraphs, convert it so each paragraph is its own line.
      *
      * Additionally, recognize Markdown-like triple-backquote lines,
      * which start and end sections whose whitespace is to be
      * transferred verbatim. */
    public static String joinAdjacentLines(String input)
    {
        if (input.isEmpty()) {
            // Splitting the empty string yields a single empty string,
            // which would then turn into a newline, so handle this
            // specially.
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // The line we saw on the previous iteration of the loop.
        String prevLine = "";

        // True if we are in a triple-backquote section.
        boolean inLiteralSection = false;

        // A limit of -1 means we will get trailing empty strings, i.e.,
        // we can see the trailing newlines.
        String[] lines = input.split("\n", -1);
        for (String line : lines) {
            if (inLiteralSection) {
                if (line.equals("```")) {
                    inLiteralSection = false;
                }
                else {
                    sb.append(line+"\n");
                }
            }
            else if (line.equals("```")) {
                sb.append("\n");
                inLiteralSection = true;
            }
            else if (line.isEmpty()) {
                sb.append("\n");
            }
            else {
                if (prevLine.endsWith(".") ||
                    prevLine.endsWith("?") ||
                    prevLine.endsWith("!"))
                {
                    // Two spaces separating a sentence terminator from
                    // the next word.
                    sb.append("  ");
                }
                else if (prevLine.endsWith(" ") ||
                         prevLine.endsWith("\t") ||
                         prevLine.equals("```") ||
                         prevLine.isEmpty()) {
                    // Don't add more space if the previous line ended
                    // with space, or wasn't there.
                }
                else {
                    // Single space separating words within a sentence.
                    sb.append(" ");
                }

                sb.append(line);
            }

            prevLine = line;
        }

        return sb.toString();
    }

    /** If 's' is not empty, return 's+toAppend'.  Otherwise return
        the empty string. */
    public static String appendIfNotEmpty(String toAppend, String s)
    {
        if (s.isEmpty()) {
            return s;
        }
        else {
            return s+toAppend;
        }
    }

    /** Return a string built by appending 'terminator' to all of the
        elements in 'coll', in its natural iteration order, and
        concatenating the result. */
    public static String joinWithTerminators(
        String terminator,
        Collection<String> coll)
    {
        StringBuilder sb = new StringBuilder();

        for (String s : coll) {
            sb.append(s);
            sb.append(terminator);
        }

        return sb.toString();
    }
}

// EOF
