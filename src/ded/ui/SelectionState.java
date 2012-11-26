// SelectionState.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

/** Possible selection states for a Controller. */
public enum SelectionState {
    SS_UNSELECTED,                 // not selected
    SS_SELECTED,                   // selected, possibly with other controllers
    SS_EXCLUSIVE                   // only selected controller, so has resize controls
}

// EOF
