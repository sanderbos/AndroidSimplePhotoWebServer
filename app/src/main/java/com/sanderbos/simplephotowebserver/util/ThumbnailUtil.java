package com.sanderbos.simplephotowebserver.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class with code related to accessing images in the meta-database
 * or creating thumbnails and other JPEGs from scratch.
 */
public class ThumbnailUtil {

    /**
     * Quality to use for converted images.
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
    public static byte[] createJPGThumbnail(String pathToImage, int widthForThumbnail) throws IOException {
        return performJPGConversion(pathToImage, widthForThumbnail, null);

    }

    /**
     * Create a new JPEG image with a rotation applied.
     *
     * @param pathToImage The image to convert.
     * @param rotation    The rotation to apply.
     * @return A byte array representing a (rotated) JPEG image.
     * @throws IOException In case the file cannot be opened, or closed.
     */
    public static byte[] createRotatedJPG(String pathToImage, ImageOrientation rotation) throws IOException {
        return performJPGConversion(pathToImage, -1, rotation);
    }

    /**
     * Create a converted image, based on a path, a new width, and an optional rotation.
     * <br>
     * (Performance characteristics indication on a Galaxy S3: 250ms for small images (&lt; 200KB), 700ms for larger images,
     * for a width of 40 pixels the images are about 2KB).
     *
     * @param pathToImage    The full path to the image, expected to represent an existing image.
     * @param thumbnailWidth The width to use for thumbnails (not used when a rotation is applied).
     * @param rotation       An optional rotation, either a rotation or a thumbnailWidht should be specified.
     * @return A byte array representing a converted JPEG image.
     * @throws IOException In case the file cannot be opened, or closed.
     */
    private static synchronized byte[] performJPGConversion(String pathToImage, int thumbnailWidth, ImageOrientation rotation) throws IOException {

        // This method is synchronized for a reason. It takes a lot of memory to construct the bitmap object, so it should be taken care
        // of that no two conversions are executed at the same time

        byte[] convertedImageData = null;

        // http://stackoverflow.com/questions/2577221/android-how-to-create-runtime-thumbnail recommends using
        // Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), THUMBSIZE, THUMBSIZE);
        // but then I must know the height.
        FileInputStream imageInputStream = new FileInputStream(pathToImage);
        try {
            //
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            if (rotation != null) {
                // Thumbnails, the scale is set to 8 for performance, must be power of 2 setting it to 16 (smaller) did not make performance better.
                bitmapOptions.inSampleSize = 8;
            } else {
                // Rotation, to lower quality reduce size somewhat on reading (not possible during rotation).
                // Sort of magic number, but for input file of 1.6MB leads to output file of
                bitmapOptions.inSampleSize = 2;
            }

            Bitmap imageBitmap = BitmapFactory.decodeStream(imageInputStream, null, bitmapOptions);
            try {

                Bitmap convertedImageBitmap;
                if (rotation == null) {
                    // Thumbnail, scale down the image
                    int scaledHeight = (int) (thumbnailWidth / ((double) imageBitmap.getWidth() / (double) imageBitmap.getHeight()));
                    convertedImageBitmap = Bitmap.createScaledBitmap(imageBitmap, thumbnailWidth, scaledHeight, false);
                } else {
                    // Rotate the image
                    Matrix conversionMatrix = new Matrix();
                    conversionMatrix.postRotate(rotation.getRotationInDegrees());
                    final boolean filter = true;
                    // Note: It is not possible to scale down the image in this action, scaling down is done with the bitmapOptions above
                    convertedImageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), conversionMatrix, filter);
                }

                try {
                    ByteArrayOutputStream jpegImageOutputStream = new ByteArrayOutputStream();
                    convertedImageBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, jpegImageOutputStream);
                    convertedImageData = jpegImageOutputStream.toByteArray();
                } finally {
                    convertedImageBitmap.recycle();
                }

                if (rotation != null && convertedImageData != null) {
                    MyLog.debug("Image converted to size {0,number,#} by {1,number,#} leading to size of {2,number,#} KB ", convertedImageBitmap.getWidth(), convertedImageBitmap.getHeight(), convertedImageData.length / 1024);
                }
            } finally {
                imageBitmap.recycle();

            }
        } finally {
            imageInputStream.close();
        }

        return convertedImageData;
    }

    /**
     * Get the width and height of an image.
     *
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

    /**
     * Determine the embedded orientation of a JPEG image. This method should only be called for
     * JPEG images, as it inspects the Exif information.
     *
     * @param pathToJpegImage The full path of the image whose EXIF data to check.
     * @return The image orientation, always returns a value (expections are handled internally),
     * ROTATE_NONE in case no value could be determined.
     */
    public static ImageOrientation getOrientationForImage(String pathToJpegImage) {
        ImageOrientation orientation = ImageOrientation.ROTATE_NONE;

        try {
            int exifOrientation = getExifOrientationForImage(pathToJpegImage);
            orientation = ImageOrientation.getImageOrientationByExifInterfaceValue(exifOrientation);
        } catch (Exception e) {
            MyLog.error("Failed to get orientation, continuing with regular orientation", e);
        }

        return orientation;
    }

    /**
     * Determine the exif orientation value for an image file.
     *
     * @param pathToImage The path to a JPEG image.
     * @return The Exif orientation value.
     * @throws IOException In case the JPEG file could not be read, or the Exif information could not
     *                     be extracted.
     */
    private static int getExifOrientationForImage(String pathToImage) throws IOException {
        ExifInterface exif = new ExifInterface(pathToImage);
        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        MyLog.debug("Found Exif image orientation for file {0}: {1}", pathToImage, exifOrientation);

        return exifOrientation;
    }

}
