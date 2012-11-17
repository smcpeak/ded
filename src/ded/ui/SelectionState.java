// SelectionState.java

package ded.ui;

/** Possible selection states for a Controller. */
public enum SelectionState {
    SS_UNSELECTED,                 // not selected
    SS_SELECTED,                   // selected, possibly with other controllers
    SS_EXCLUSIVE                   // only selected controller
}

// EOF
