// UndoHistoryLimit.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** This interface is used by UndoHistory to get and set the history
  * length limit.
  *
  * Currently, the only implementor is DiagramController, but I am
  * using the interface to keep the Undo functionality somewhat
  * decoupled from the rest of the UI. */
public interface UndoHistoryLimit {
    /** Get the current undo history limit.  0 means no limit.
      *
      * A limit of N means the path from the current state to its
      * oldest ancestor contains no more than N states.  Thus,
      * a limit of 1 means there are no states beyond the current.
      *
      * Note that, in theory, this does not bound the total number
      * of states because there is no bound on the branching factor. */
    int getUndoHistoryLimit();

    /** Set the limit.  It is interpreted as specified with
      * 'getLimit()'.
      *
      * The limit is set in response to a user interaction, so it is
      * desirable that the limit be persistent in some sense.
      * However, all that is required by this interface is that a
      * subsequent call to 'getLimit' on the same instance will
      * return the newly set limit. */
    void setUndoHistoryLimit(int newLimit);
}

// EOF
