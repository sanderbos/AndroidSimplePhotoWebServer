package com.sanderbos.simplephotowebserver.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class with code related to accessing thumbnails in the meta-database
 * or creating thumbnails from scratch.
 */
public class ThumbnailUtil {

    /**
     * Quality to use for image
     */
    private static final int JPEG_COMPRESSION_QUALITY = 80;


    /**
     * Create a thumbnail image, based on a path.
     * <br>
     * (Performance characteristics indication on a Galaxy S3: 250ms for small images (&lt; 200KB), 700ms for larger images,
     * for a width of 40 pixels the images are about 2KB).
     *
     * @param pathToImage       The full path to the image, expected to represent an existing image.
     * @param widthForThumbnail The width to use for the thumbnail, the appropriate height is calculated.
     * @return A byte array representing a JPG thumbnail.
     * @throws IOException In case the file cannot be opened, or closed.
     */
    public static synchronized byte[] createJPGThumbnail(String pathToImage, int widthForThumbnail) throws IOException {

        // This method is synchronized for a reason. It takes a lot of memory to construct the bitmap object, so it should be taken care
        // of that no two

        byte[] thumbnailData = null;

        // http://stackoverflow.com/questions/2577221/android-how-to-create-runtime-thumbnail recommends using
        // Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), THUMBSIZE, THUMBSIZE);
        // but then I must know the height.
        FileInputStream imageInputStream = new FileInputStream(pathToImage);
        try {
            Bitmap imageBitmap = BitmapFactory.decodeStream(imageInputStream);
            try {
                int scaledHeight = (int) (widthForThumbnail / ((double) imageBitmap.getWidth() / (double) imageBitmap.getHeight()));

                Bitmap thumbnailImageBitmap = Bitmap.createScaledBitmap(imageBitmap, widthForThumbnail, scaledHeight, false);

                try {
                    ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
                    thumbnailImageBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, thumbnailOutputStream);
                    thumbnailData = thumbnailOutputStream.toByteArray();
                } finally {
                    thumbnailImageBitmap.recycle();
                }
            } finally {
                imageBitmap.recycle();

            }
        } finally {
            imageInputStream.close();
        }

        return thumbnailData;
    }
}
