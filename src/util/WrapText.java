// WrapText.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.ArrayList;
import java.util.List;

import util.StringMeasurer;
import util.WrapTextPolicy;


/** Produce a wrapped version of original text. */
public class WrapText {
    /** Return a wrapped version of 'origText', which is presumed to
        already contain newlines to be retained, by inserting additional
        newlines, trying to ensure all the lines fit in 'maxWidth'
        according to 'measurer'.

        The output is the same as 'origText', except that some newlines
        may have been inserted and some non-newline whitespace removed.

        If 'indentSpaces' is not zero, then when a line is broken, that
        many spaces are also added at the start of the new line.

        If there are not enough places to break the text, the output
        will have lines longer than 'maxWidth'.

        The algorithm is greedy, putting as much text onto each line as
        will fit before considering the next.
    */
    public static String wrapText(
        WrapTextPolicy policy,
        int maxWidth,
        int indentSpaces,
        String origText,
        StringMeasurer measurer)
    {
        if (policy == WrapTextPolicy.WTP_NoWrap) {
            return origText;
        }

        String[] origLines = origText.split("\n", -1);

        List<String> wrappedLines = new ArrayList<String>();
        for (String origLine : origLines) {
            wrappedLines.add(wrapLine(policy, maxWidth, indentSpaces,
                                      origLine, measurer));
        }

        return String.join("\n", wrappedLines);
    }

    /** Wrap a single line of text. */
    public static String wrapLine(
        WrapTextPolicy policy,
        int maxWidth,
        int indentSpaces,
        String origLine,
        StringMeasurer measurer)
    {
        // Maybe no wrapping is needed?
        if (policy == WrapTextPolicy.WTP_NoWrap ||
            measurer.substringWidth(origLine, 0, origLine.length()) <= maxWidth)
        {
            return origLine;
        }

        // Number of pixels required for the indented lines.  This is
        // set to a nonzero value after we add the first line break.
        int indentWidth = 0;

        // The string for 'indentSpaces'.
        String indentString = StringUtil.repeatString(" ", indentSpaces);

        // Accumulator for the output.
        StringBuilder sb = new StringBuilder();

        // The index of the start of the text we need to fit onto the
        // next line.
        int curStart = 0;

        // Outer loop to consume the entire line.
        while (curStart < origLine.length()) {

            // The most recently found split that would stay under the limit.
            int curSplit = curStart;

            // Inner loop to find the next split point.
            while (curSplit < origLine.length()) {
                // Find the next place we could break the line.
                int newSplit = nextSplit(policy, origLine, curSplit+1);
                if (measurer.substringWidth(origLine, curStart, newSplit)
                        + indentWidth <= maxWidth) {
                    // Breaking here would stay under the limit, so this
                    // becomes our new best candidate.
                    curSplit = newSplit;
                }
                else {
                    // Have to split.
                    if (curSplit == curStart) {
                        // We do not have a previous candidate, so split
                        // at 'newSplit', even though the line will be
                        // over the limit.
                        curSplit = newSplit;
                    }
                    break;
                }
            }

            // Add everything up to the chosen split point.
            sb.append(origLine, curStart, curSplit);

            // Discard the whitespace that follows the split point.
            curStart = nextNonWS(origLine, curSplit);
            if (curStart == origLine.length()) {
                // The remaining text is all whitespace.
                break;
            }
            else {
                // There is still more text, so insert a newline to
                // actually break the line.
                sb.append('\n');

                if (indentSpaces > 0) {
                    sb.append(indentString);
                    if (indentWidth == 0) {
                        indentWidth = measurer.substringWidth(
                            indentString, 0, indentSpaces);
                    }
                }
            }
        }

        return sb.toString();
    }

    /** Find the next candidate split point after 'start'.  If there
        is none, return 'origLine.length()'. */
    private static int nextSplit(
        WrapTextPolicy policy,
        String origLine,
        int start)
    {
        // It doesn't make any sense to break the line at the start.
        assert(start > 0);

        // We should not get here if the policy is to not wrap.
        assert(policy != WrapTextPolicy.WTP_NoWrap);

        int i = start;
        for (; i < origLine.length(); ++i) {
            if (canSplitAfter(policy, origLine.charAt(i-1)) &&
                canSplitBefore(policy, origLine.charAt(i)))
            {
                return i;
            }
        }

        // Return the string length.
        return i;
    }

    /** Return the index of the first character at or after 'i' that is
        not whitespace.  Return 'line.length()' if there is none. */
    private static int nextNonWS(
        String line,
        int i)
    {
        while (i < line.length() &&
               Character.isWhitespace(line.charAt(i))) {
            ++i;
        }
        return i;
    }

    /** True if we can split at a point right after 'ch'. */
    private static boolean canSplitAfter(
        WrapTextPolicy policy,
        char ch)
    {
        switch (policy) {
            case WTP_NoWrap:
                return false;

            case WTP_Sentence:
                return ".?!:;".indexOf(ch) >= 0;

            case WTP_Whitespace:
                return !Character.isWhitespace(ch);
        }

        return false;      // Not reachable.
    }

    /** True if we can split at a point right before (at) 'ch'. */
    private static boolean canSplitBefore(
        WrapTextPolicy policy,
        char ch)
    {
        switch (policy) {
            case WTP_NoWrap:
                return false;

            case WTP_Sentence:
                return Character.isWhitespace(ch);

            case WTP_Whitespace:
                return Character.isWhitespace(ch);
        }

        return false;      // Not reachable.
    }
}


// EOF
