// JSONable.java
// See toplevel license.txt for copyright and license terms.

package util.json;

import org.json.JSONObject;

/** Something that can be serialized as JSON using the org.json implementation. */
public interface JSONable {
    /** Return a JSON representation of the object. */
    JSONObject toJSON();
}

// EOF
