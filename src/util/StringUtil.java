// StringUtil.java

package util;

/** General-purpose string utilities. */
public class StringUtil {
    /** Return 's' as a quoted JSON syntax using only ASCII characters. */
    public static String quoteAsJSONASCII(String s)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i=0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (32 <= c && c <= 126) {
                sb.append(c);
            }
            else {
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
                        sb.append(String.format("\\u%04X", (int)c));
                        break;
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}

// EOF
