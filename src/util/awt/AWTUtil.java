// AWTUtil.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

/** Miscellaneous utilities for use with AWT. */
public class AWTUtil {
    /** Print to stdout the tree of components/containers rooted at 'c'. */
    public static void dumpComponentTree(Component c, int indent)
    {
        for (int i=0; i<indent; i++) {
            System.out.print("  ");
        }
        System.out.print("type="+c.getClass().getSimpleName()+
                         " name="+c.getName()+
                         " x="+c.getX()+
                         " y="+c.getY()+
                         " w="+c.getWidth()+
                         " h="+c.getHeight());

        if (c instanceof Container) {
            Container ctr = (Container)c;
            Component[] children = ctr.getComponents();
            if (children.length == 0) {
                System.out.println(" {}");
            }
            else {
                System.out.println(" {");

                // Print the children backwards, so they are printed from
                // back to front.  That way, in the context of the complete
                // hierarchy, everything is back to front (containers are
                // conceptually behind their children).
                for (int i= children.length - 1; i >= 0; i--) {
                    dumpComponentTree(children[i], indent+1);
                }

                for (int i=0; i<indent; i++) {
                    System.out.print("  ");
                }
                System.out.println("}");
            }
        }
        else {
            System.out.println();
        }
    }

    /** Print to stdout all the Frames and their Component trees. */
    public static void dumpFrameTrees()
    {
        System.out.println("Frames:");
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
            dumpComponentTree(f, 1);
        }
    }
}

// EOF
