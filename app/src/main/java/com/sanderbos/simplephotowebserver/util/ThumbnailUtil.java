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
            // The scale is set to 8 for performance, must be power of 2 setting it to 16 (smaller) did not make performance better.
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = 8;

            Bitmap imageBitmap = BitmapFactory.decodeStream(imageInputStream, null, bitmapOptions);
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

    /**
     * Get the width and height of an image.
     * @param pathToImage The image whose dimensions to get.
     * @return An integer array with the width (index 0 in result) and height (index 1 in result) of an
     * image.
     * @throws IOException In case the image cannot be opened.
     */
    public static int[] getDimensions(String pathToImage) throws IOException {

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathToImage, bitmapOptions);
        int[] result = new int[2];
        result[0] = bitmapOptions.outWidth;
        result[1] = bitmapOptions.outHeight;
        return result;
    }
}
