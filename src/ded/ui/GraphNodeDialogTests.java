// GraphNodeDialogTests.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ded.ui.GraphNodeDialog;
import ded.ui.GraphNodeDialog.TableEntry;

public class GraphNodeDialogTests {
    public static void main(String[] args)
    {
        GraphNodeDialogTests t = new GraphNodeDialogTests();
        t.insertInMiddle();
        t.reverse();
        t.removeTwo();
        t.removeTwoInitReverse();
        t.addTwoToEmpty();
    }

    /** Make an entry for testing purposes.  In this algorithm, the
      * 'isAttribute' and 'value' fields do not matter, so they are set
      * to arbitrary values. */
    private TableEntry makeEntry(
        Float ordinal, String key)
    {
        return new TableEntry(
            true, ordinal, key, "");
    }

    /** Check that one invocation produces the expected result. */
    private void check(
        List<TableEntry> entries,
        List<String> oldShowFields,
        List<String> expect)
    {
        List<String> actual =
            GraphNodeDialog.computeNewShowFields(entries, oldShowFields);
        if (!actual.equals(expect)) {
            System.err.println("GraphNodeDialogTests failed");
            assert(false);
            System.exit(2);
        }
    }

    private void insertInMiddle()
    {
        List<String> oldShowFields = Arrays.asList("a", "b", "c");

        List<TableEntry> entries = Arrays.asList(
            makeEntry(null, "unselected"),
            makeEntry(0.5f, "c"),
            makeEntry(null, "unselected2"));

        List<String> expect = Arrays.asList("a", "c", "b");

        check(entries, oldShowFields, expect);
    }

    private void reverse()
    {
        List<String> oldShowFields = Arrays.asList("a", "b", "c");

        List<TableEntry> entries = Arrays.asList(
            makeEntry(2f, "a"),
            makeEntry(0.5f, "b"),
            makeEntry(-3f, "c"));

        List<String> expect = Arrays.asList("c", "b", "a");

        check(entries, oldShowFields, expect);
    }

    private void removeTwo()
    {
        List<String> oldShowFields = Arrays.asList("a", "b", "c", "d");

        List<TableEntry> entries = Arrays.asList(
            makeEntry(null, "b"),
            makeEntry(null, "c"));

        List<String> expect = Arrays.asList("a", "d");

        check(entries, oldShowFields, expect);
    }

    private void removeTwoInitReverse()
    {
        List<String> oldShowFields = Arrays.asList("d", "c", "b", "a");

        List<TableEntry> entries = Arrays.asList(
            makeEntry(null, "b"),
            makeEntry(null, "c"));

        List<String> expect = Arrays.asList("d", "a");

        check(entries, oldShowFields, expect);
    }

    private void addTwoToEmpty()
    {
        List<String> oldShowFields = Arrays.asList();

        List<TableEntry> entries = Arrays.asList(
            makeEntry(null, "a"),
            makeEntry(3f, "b"),
            makeEntry(2f, "c"));

        List<String> expect = Arrays.asList("c", "b");

        check(entries, oldShowFields, expect);
    }
}


// EOF
