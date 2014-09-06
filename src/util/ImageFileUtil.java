// ImageFileUtil.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Utilities related to manipulating image files. */
public class ImageFileUtil {
    /** Write the 'bi' to 'file' in PNG format. */
    public static void writeImageToPNGFile(BufferedImage bi, File file)
        throws Exception
    {
        // This method's code is approximately equivalent to:
        //   ImageIO.write(bi, "png", file);
        // except that its file error handling is not atrocious,
        // it works correctly when 'file' is a symlink,
        // and I can extend it in the future to write PNG comments.

        // Get a writer than can write PNG.
        ImageWriter writer;
        {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            if (!writers.hasNext()) {
                throw new RuntimeException(
                    "Unable to write PNG image because there is no registered "+
                    "image writer for that format.  That might mean the Java "+
                    "run time library is incomplete somehow.");
            }
            writer = writers.next();
        }

        try {
            // Open the output file.
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            try {
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                if (ios == null) {
                    throw new RuntimeException("ImageIO.createImageOutputStream(OutputStream) failed!");
                }

                try {
                    // Write the image data to that file.
                    writer.setOutput(ios);
                    writer.write(bi);
                }
                finally {
                    ios.close();
                }
            }
            finally {
                // You have to dig into the source code to discover this, but
                // closing 'ios' will *not* close 'os'.
                os.close();
            }
        }
        finally {
            writer.dispose();
        }
    }
}
