// ImageFileUtil.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Utilities related to manipulating image files. */
public class ImageFileUtil {
    /** Write the 'bi' to 'file' in PNG format.
      *
      * If 'comment' is not null, it will be added as an image comment.
      * However, it must only contain ASCII characters.
      *
      * If this fails, it will throw an Exception.  If it succeeds
      * but there is a warning, that warning will be returned as a
      * String.  If there is no warning, then null is returned. */
    public static String writeImageToPNGFile(BufferedImage bi, File file, String comment)
        throws Exception
    {
        String warningReturn = null;

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
            // Set up the metadata structures that will allow setting
            // a text chunk comment.
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            ImageTypeSpecifier its = new ImageTypeSpecifier(bi);
            IIOMetadata iiomd = writer.getDefaultImageMetadata(its, iwp);

            // Possibly add the comment.
            if (comment != null) {
                try {
                    // PNG text chunk keywords are explained at:
                    // http://www.libpng.org/pub/png/spec/iso/index-object.html#11keywords
                    addPNGTextChunk(iiomd, "Comment", comment, true /*compressed*/);
                }
                catch (Exception e) {
                    warningReturn = "Failed to add PNG text chunk comment: " +
                                    Util.getExceptionMessage(e);
                }
            }

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
                    writer.write(new IIOImage(bi, null, iiomd));
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

        return warningReturn;
    }

    /** Add a PNG "tEXt" (compressed=false) or "zTXt" (compressed=true)
      * chunk to 'iiomd'. */
    public static void addPNGTextChunk(IIOMetadata iiomd, String keyword,
                                       String value, boolean compressed)
        throws Exception
    {
        // Check that the comment only uses ASCII characters, since
        // otherwise the comment will be corrupted.
        //
        // It would be natural here to use "iTXt", since that would allow
        // all Unicode characters to be used.  However, I can see that there
        // is a bug in the imageio library:
        //
        // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/com/sun/imageio/plugins/png/PNGImageWriter.java#PNGImageWriter.write_iTXt%28%29
        //
        // because if the compression flag is set, then the characters are
        // simply truncated to [0,255] prior to compression.
        {
            for (int i=0; i < value.length(); i++) {
                int c = value.charAt(i);
                if (c >= 127) {
                    throw new RuntimeException(String.format(
                        "Comment contains non-ASCII character U+%04X, "+
                        "so it cannot be represented in a PNG comment.", c));
                }
            }
        }

        // Construct a magic XML document that can be "merged"
        // into the metadata to create a text chunk.  The only way
        // to know to do this is to read the source code of
        // com.sun.imageio.plugins.png.PNGMetadata, and/or follow
        // an example from someone else like:
        // http://stackoverflow.com/questions/6495518/writing-image-metadata-in-java-preferably-png
        String metadataFormatName = "javax_imageio_png_1.0";
        IIOMetadataNode commentMetadata;
        {
            IIOMetadataNode textEntry = new IIOMetadataNode(compressed? "zTXtEntry" : "tEXtEntry");
            textEntry.setAttribute("keyword", keyword);
            textEntry.setAttribute((compressed? "text" : "value"), value);
            if (compressed) {
                // There is only one compression method defined for "zTXt"
                // chunks.  The magic string "deflate" is embedded in the
                // code that processes this magic XML document, and must
                // be specified, otherwise writing the PNG throws an
                // exception.
                textEntry.setAttribute("compressionMethod", "deflate");
            }

            IIOMetadataNode text = new IIOMetadataNode(compressed? "zTXt" : "tEXt");
            text.appendChild(textEntry);

            commentMetadata = new IIOMetadataNode(metadataFormatName);
            commentMetadata.appendChild(text);

            // At this point, 'commentMetadata' looks like this:
            //
            //   <javax_imageio_png_1.0>
            //     <tEXt>
            //       <tEXtEntry keyword="$KEYWORD" value="$VALUE" />
            //     </tEXt>
            //   </javax_imageio_png_1.0>
            //
            // or:
            //
            //   <javax_imageio_png_1.0>
            //     <zTXt>
            //       <zTXtEntry keyword="$KEYWORD" text="$VALUE" compressionMethod="deflate" />
            //     </zTXt>
            //   </javax_imageio_png_1.0>
        }

        // Merge it into 'iiomd'.  Internally, this reads the XML
        // document and copies the values it recognizes into fields
        // of the PNGMetadata object, which will then be added to
        // the output PNG file when it is written.
        iiomd.mergeTree(metadataFormatName, commentMetadata);
    }
}
