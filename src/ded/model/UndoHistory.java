// UndoHistory.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.ArrayList;

import util.StringUtil;

/** Maintain a history of diagram changes to support undo and redo. */
public class UndoHistory {
    // ---- types ----
    /** One diagram state, with links to history and (redo) future. */
    private static class HistoryEntry {
        // ---- data ----
        /** The state of the diagram at this point in the history.
          * Each Diagram is a deep copy; there is no sharing among
          * entries or with the Diagram being actively edited. */
        public Diagram diagram;

        /** The state from which a user issued a top-level command
          * to obtain this state.  It may be null, meaning we do not
          * know what came before (because the editor was just started,
          * or we chose to truncate the history for space reasons). */
        public HistoryEntry parent;

        /** Localized command string describing how 'diagram' was
          * obtained from 'parent'. */
        public String commandDescription;

        /** Redo futures, in the order they were created or resumed.  It might
          * be empty if this is the last entry.  Otherwise, the first
          * entry represents the "first" future the user created.  If
          * they then undo back to this state and then make a new set
          * of changes, that starts the "second" future, etc.  If
          * the user then reverts and resumes an older future, that
          * older future is moved to the end, with other futures
          * sliding back one spot to maintain contiguity. */
        public ArrayList<HistoryEntry> futures = new ArrayList<HistoryEntry>();

        // ---- methods ----
        public HistoryEntry(Diagram d, HistoryEntry p, String c)
        {
            this.diagram = d;
            this.parent = p;
            this.commandDescription = c;
        }
    }

    // ---- data ----
    /** When true, print diagnostics to stdout. */
    private static final boolean debug = false;

    /** The current state of the editor.  In between user edits, its
      * diagram should always be equals() to the Diagram in the editor. */
    private HistoryEntry current;

    /** Interface to get the history size limit. */
    private UndoHistoryLimit undoHistoryLimit;

    // ---- methods ----
    /** Initialize a new undo history.  This will make its own deep
      * copy of 'initDiagram'. */
    public UndoHistory(Diagram initDiagram, String initCommandDesc, UndoHistoryLimit uhl)
    {
        this.current = new HistoryEntry(initDiagram.deepCopy(), null, initCommandDesc);
        this.undoHistoryLimit = uhl;

        if (debug) {
            System.out.println("constructor: "+initCommandDesc);
            System.out.print(this.dumpHistoryStructure());
        }
    }

    /** Record a change to the diagram (*not* made via undo or redo).
      * This method makes a deep copy of 'newDiagram'. */
    public void recordDiagramChange(Diagram newDiagram, String commandDesc)
    {
        HistoryEntry newEntry =
            new HistoryEntry(newDiagram.deepCopy(), this.current, commandDesc);
        this.current.futures.add(newEntry);
        this.current = newEntry;

        // Apply the history limit, which restricts the number of states
        // on the path from current to oldest ancestor.
        int limit = this.undoHistoryLimit.getUndoHistoryLimit();
        if (limit > 0) {
            HistoryEntry ancestor = newEntry;
            limit--;     // Count the current state against the limit.
            while (limit > 0 && ancestor != null) {
                ancestor = ancestor.parent;
                limit--;
            }
            if (ancestor != null) {
                // We hit the limit.  Discard any history before 'ancestor'.
                ancestor.parent = null;
            }
        }

        if (debug) {
            System.out.println("recordDiagramChange: "+commandDesc);
            System.out.print(this.dumpHistoryStructure());
        }
    }

    /** Return the Diagram in the current state.  This is *not* a deep
      * copy (for efficiency); it is only meant for the caller to
      * compare their state to as a self-check.  If this were C++, I
      * would return it as a pointer or reference to const. */
    public Diagram getCurrentDiagram()
    {
        return this.current.diagram;
    }

    /** Return true if it is possible to undo in this state. */
    public boolean canUndo()
    {
        return this.current.parent != null;
    }

    /** Return true if it is possible to redo in this state. */
    public boolean canRedo()
    {
        return numRedos() > 0;
    }

    /** Return number of possible redo actions in this state.
      * If the return is 0, it is not possible to redo. */
    public int numRedos()
    {
        return this.current.futures.size();
    }

    /** Describe each of the possible redos.  String 0 is the
      * description of redo 0, and so on.  They are listed in
      * order of creation or resumption, whichever is later
      * for that entry.  The size of the returned list is equal
      * to 'numRedos()'. */
    public ArrayList<String> describeRedos()
    {
        ArrayList<String> ret = new ArrayList<String>();
        for (HistoryEntry f : this.current.futures) {
            ret.add(f.commandDescription);
        }
        return ret;
    }

    /** Perform an undo.  canUndo() must be true.  The returned diagram
      * is a deep copy of the one maintained in the history, so the
      * caller can freely take ownership of it. */
    public Diagram undo()
    {
        assert(this.canUndo());
        Diagram ret = this.current.parent.diagram.deepCopy();
        this.current = this.current.parent;

        if (debug) {
            System.out.println("UNDO");
            System.out.print(this.dumpHistoryStructure());
        }

        return ret;
    }

    /** Perform a redo.  The argument 'which' must be at least 0, and
      * less than 'numUndos()'.  The returned diagram is a deep copy
      * of the one maintained in the history. */
    public Diagram redo(int which)
    {
        assert(0 <= which && which < this.numRedos());
        HistoryEntry resumed = this.current.futures.get(which);
        assert(resumed.parent == this.current);

        if (which != this.numRedos()-1) {
            // We are resuming a future that is not the most recent.
            // Move it to the end.
            this.current.futures.remove(which);
            this.current.futures.add(resumed);
        }

        this.current = resumed;

        if (debug) {
            System.out.println("REDO");
            System.out.print(this.dumpHistoryStructure());
        }

        return this.current.diagram.deepCopy();
    }

    /** Redo the most recently resumed future. */
    public Diagram redoMostRecent()
    {
        return redo(this.current.futures.size()-1);
    }

    /** Return a debug string that describes the currently stored history. */
    public String dumpHistoryStructure()
    {
        StringBuilder sb = new StringBuilder();

        // First, find the root of the history.
        HistoryEntry root = this.current;
        {
            // Use the fast-slow trick to detect an infinite loop.
            HistoryEntry slower = root;
            while (root.parent != null) {
                root = root.parent;
                slower = slower.parent;

                // 'root' jumps two spots for every one of 'slower'.
                if (root.parent != null) {
                    root = root.parent;
                    if (root == slower) {
                        assert(false);
                        return "History parent chain has a cycle!";
                    }
                }
            }
        }

        // Now walk the history forward, printing it to 'sb'.
        this.printHistory(sb, 0 /*indent*/, root);

        return sb.toString();
    }

    /** Print the history starting from 'entry'. */
    private void printHistory(StringBuilder sb, int indent, HistoryEntry entry)
    {
        // First print 'entry' itself.
        sb.append(StringUtil.indent(indent));
        if (entry == this.current) {
            sb.append("* ");
        }
        else {
            sb.append("- ");
        }
        sb.append(entry.commandDescription);
        if (entry.parent != null &&
            !entry.diagram.windowSize.equals(entry.parent.diagram.windowSize))
        {
            // Changes in diagram size are not recorded as separate
            // actions due to technical limitations in the editor's
            // ability to track a complete resize action (as opposed
            // to all the intermediate states).  In effect, the size
            // changes are lumped in with the next non-size change.
            // So, add a note to the label to acknowledge that.
            int w = entry.diagram.windowSize.width;
            int h = entry.diagram.windowSize.height;
            sb.append(" and resize to ("+w+","+h+")");
        }
        sb.append(": (e="+entry.diagram.entities.size()+
                  ", i="+entry.diagram.inheritances.size()+
                  ", r="+entry.diagram.relations.size()+")\n");

        // Stop if there are no futures beyond this.
        if (entry.futures.isEmpty()) {
            return;
        }

        // If there are multiple futures, print the ones that are not
        // the most recently resumed first.
        for (int i=0; i < entry.futures.size()-1; i++) {
            sb.append(StringUtil.indent(indent+1));
            sb.append("> alternate future "+(i+1)+":\n");
            this.printHistory(sb, indent+2, entry.futures.get(i));
        }

        // Then print the main future last, at the same indentation as
        // its parent.
        //
        // Note: Although I have cycle detection for going back in
        // history, there is currently no cycle detection going forward.
        this.printHistory(sb, indent, entry.futures.get(entry.futures.size()-1));
    }
}

// EOF
