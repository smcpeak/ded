// ResourceImageCache.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Image;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;

/** Cache Images loaded from resources. */
public class ResourceImageCache {
    // -------------- instance data ----------------
    /** Map from resource name to its Image, or null if it could
      * not be loaded. */
    private HashMap<String, Image> cache;

    // ---------------- methods --------------------
    public ResourceImageCache()
    {
        this.cache = new HashMap<String, Image>();
    }

    /** Retrieve an image resource called 'resourceName'.  This name
      * should be relative to the "resources" directory.  It will be
      * loaded from the active JAR files or from the file system,
      * depending on where it can be found.  If it cannot be found or
      * there is a loading error, returns null. */
    public Image getResourceImage(String resourceName)
    {
        // Consult the cache.
        if (this.cache.containsKey(resourceName)) {
            return this.cache.get(resourceName);  // Might be null.
        }

        // Try to load the image from disk.
        Image image = this.innerGetResourceImage(resourceName);

        // Cache the result, whatever it was, even if null.
        this.cache.put(resourceName, image);

        return image;
    }

    /** Retrieve 'resourceName', ignoring the cache. */
    private Image innerGetResourceImage(String resourceName)
    {
        InputStream in = null;
        try {
            // Try loading from JAR file.
            URL url = ResourceImageCache.class.getResource("/resources/"+resourceName);
            if (url != null) {
                in = url.openStream();
            }
            else {
                // Try loading from file system.  This will throw if
                // the file cannot be read.
                in = new FileInputStream("resources/"+resourceName);
            }

            // Parse the image data.
            Image image = ImageIO.read(in);
            if (image == null) {
                System.err.println(
                    "no registered image reader for: "+resourceName);
                return null;
            }

            return image;
        }
        catch (Exception e) {
            System.err.println(
                "while loading \""+resourceName+"\": "+
                e.getClass().getSimpleName()+": "+e.getMessage()+"\n");
            return null;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {/*ignore*/}
            }
        }
    }
}
