// DedCmdLine.java
// See toplevel license.txt for copyright and license terms.

package ded;

import ded.Ded;
import util.StringUtil;
import util.Util;
import util.XParse;

import static util.StringUtil.fmt;


/** Parser for the 'ded' command line. */
public class DedCmdLine {
    // ---- public instance data ----
    /** True to check the object graph against the diagram. */
    public boolean m_checkGraph = false;

    /** True to check the object graph against its source file. */
    public boolean m_checkGraphSource = false;

    /** If not null, the name of the diagram file to load initially. */
    public String m_diagramFname = null;

    // ---- methods ----
    /** Parse 'args', populating the member data.  The "--help" and
        "--version" options are processed immediately, exiting in the
        process. */
    public DedCmdLine(String[] args)
        throws XParse
    {
        boolean parsingOptions = true;
        for (String arg : args) {
            if (parsingOptions) {
                if (arg.startsWith("--")) {
                    parseOption(arg);
                    continue;
                }
                else {
                    parsingOptions = false;
                }
            }

            if (m_diagramFname == null) {
                m_diagramFname = arg;
            }
            else {
                throw new XParse("Too many file names on command line.");
            }
        }

        if (hasCheckGraphOption() && m_diagramFname == null) {
            throw new XParse("The --check-graph and --check-graph-source "+
                             "options require a file name argument.");
        }
    }

    /** True if "--check-graph" or "--check-graph-source" was used. */
    public boolean hasCheckGraphOption()
    {
        return m_checkGraph || m_checkGraphSource;
    }

    /** Parse a single option argument. */
    private void parseOption(String opt)
        throws XParse
    {
        if (opt.equals("--help")) {
            System.out.print(Util.readResourceString(
                "/resources/helptext/cmdline-help.txt"));
            System.exit(0);
        }

        else if (opt.equals("--version")) {
            System.out.print(Ded.getVersions());
            System.exit(0);
        }

        else if (opt.equals("--check-graph")) {
            m_checkGraph = true;
        }

        else if (opt.equals("--check-graph-source")) {
            m_checkGraphSource = true;
        }

        else {
            throw new XParse(fmt(
                "Unrecognized option: %s.",
                StringUtil.doubleQuote(opt)));
        }
    }
}


// EOF
