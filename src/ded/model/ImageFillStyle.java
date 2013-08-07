// ImageFillStyle.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Ways that an image can fill a rectangle. */
public enum ImageFillStyle {
    IFS_UPPER_LEFT
      ("One copy in upper left"),
    IFS_STRETCH
      ("Stretch image to fill space"),
    IFS_TILE
      ("Tile image to fill space"),
    IFS_LOCK_SIZE
      ("Lock entity size to match image"),
    IFS_CENTER
      ("One copy centered horiz and vert");

    /** How to describe this style in the UI. */
    public final String description;

    ImageFillStyle(String d)
    {
        this.description = d;
    }

    @Override
    public String toString()
    {
        return this.description;
    }
}

// EOF
