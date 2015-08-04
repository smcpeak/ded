// ShapeFlag.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.EnumSet;
import java.util.Iterator;

/** Set of flags that influence shape display.  Each flag generally
  * influences only a small set of shapes. */
public enum ShapeFlag {
    // For Window.
    SF_HAS_MAXIMIZE
      ("Has maximize button",                    true,  EntityShape.ES_WINDOW),
    SF_HAS_MINIMIZE
      ("Has a minimize button",                  true,  EntityShape.ES_WINDOW),
    SF_HAS_CLOSE
      ("Has a close button",                     true,  EntityShape.ES_WINDOW),
    SF_HAS_WINDOW_OPS
      ("Has a window operations menu",           true,  EntityShape.ES_WINDOW),

    // Check box and radio button.
    SF_CHECKED
      ("Checked",                                false, EntityShape.ES_CHECK_BOX, EntityShape.ES_RADIO_BUTTON),
    SF_TRI_STATE
      ("Tri-state",                              false, EntityShape.ES_CHECK_BOX, EntityShape.ES_RADIO_BUTTON);

    /** Name of this shape as shown in the UI. */
    public final String displayName;

    /** True if the default state has this flag set. */
    public final boolean isDefault;

    /** Set of shapes to which the flag applies. */
    public final EntityShape[] shapes;

    ShapeFlag(String displayName, boolean isDefault, EntityShape... shapes)
    {
        this.displayName = displayName;
        this.isDefault = isDefault;
        this.shapes = shapes;
    }

    /** Set of all values. */
    public static EnumSet<ShapeFlag> allFlags()
    {
        return EnumSet.allOf(ShapeFlag.class);
    }

    /** Set of all values that apply to a given shape. */
    public static EnumSet<ShapeFlag> allFlagsForShape(EntityShape shape)
    {
        EnumSet<ShapeFlag> ret = EnumSet.noneOf(ShapeFlag.class);
        for (ShapeFlag flag : allFlags()) {
            // For the moment, each flag only applies to a small number of
            // shapes, so this iteration is not too slow.
            for (EntityShape s : flag.shapes) {
                if (s == shape) {
                    ret.add(flag);
                    break;
                }
            }
        }
        return ret;
    }

    /** Set of default values for a given shape. */
    public static EnumSet<ShapeFlag> defaultFlagsForShape(EntityShape shape)
    {
        EnumSet<ShapeFlag> ret = allFlagsForShape(shape);
        Iterator<ShapeFlag> it = ret.iterator();
        while (it.hasNext()) {
            if (!it.next().isDefault) {
                it.remove();
            }
        }
        return ret;
    }

    /** This is what JComboBox uses when it draws the items. */
    @Override
    public String toString()
    {
        return this.displayName;
    }
}
