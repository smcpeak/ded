// JSONUtilTests.java
// See toplevel license.txt for copyright and license terms.

package util.json;

import org.json.JSONObject;

import util.Util;

import util.json.JSONUtil;


/** Tests for JSONUtil. */
public class JSONUtilTests {
    public static void main(String[] args)
    {
        JSONUtilTests t = new JSONUtilTests();

        // For the moment, the only test is an interactive test.
        if (args.length > 0) {
            t.testReadFile(args[0]);
        }
    }

    void testReadFile(String fname)
    {
        try {
            JSONObject o = JSONUtil.readObjectFromFileName(fname);
            System.out.println(o.toString(2 /*indent*/));
        }
        catch (Exception e) {
            System.err.println(Util.getExceptionMessage(e));
            System.exit(2);
        }
    }
}


// EOF
