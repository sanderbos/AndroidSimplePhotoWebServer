package com.sanderbos.simplephotowebserver.util;

import android.media.ExifInterface;

/**
 * Enumeration of possible image rotation values, along with ExifInterface constant references and the rotation in degrees.
 */
public enum ImageOrientation {

    ROTATE_NONE(ExifInterface.ORIENTATION_NORMAL, 0),
    ROTATE_90(ExifInterface.ORIENTATION_ROTATE_90, 90),
    ROTATE_180(ExifInterface.ORIENTATION_ROTATE_180, 180),
    ROTATE_270(ExifInterface.ORIENTATION_ROTATE_270, 270),;

    /**
     * This ImageOrientation value in ExifInterface constant terms.
     */
    private int exifRotation;

    /**
     * The rotation in degrees.
     */
    private int rotationDegrees;

    /**
     * Constructor for enum values.
     *
     * @param exifRotation    The ExifInterface value.
     * @param rotationDegrees The rotation in degrees.
     */
    ImageOrientation(int exifRotation, int rotationDegrees) {
        this.exifRotation = exifRotation;
        this.rotationDegrees = rotationDegrees;
    }

    /**
     * Get the ExifInterface constant value for this enum value.
     *
     * @return the exif interface value.
     */
    public int getExifInterfaceConstantValue() {
        return exifRotation;
    }

    /**
     * Get the rotation in degrees for this enum value.
     *
     * @return The rotation in degrees that this enum value represents.
     */
    public int getRotationInDegrees() {
        return rotationDegrees;
    }

    /**
     * Get the enum value based for a given ExifInterface rotation constant value.
     *
     * @param exifRotation The integer value of an ExifInterface rotation constant.
     * @return The matching enum value, or null in case the exifRotation parameter cannot be matched.
     */
    public static ImageOrientation getImageOrientationByExifInterfaceValue(int exifRotation) {
        ImageOrientation result = null;
        for (ImageOrientation value : values()) {
            if (value.getExifInterfaceConstantValue() == exifRotation) {
                result = value;
                break;
            }
        }
        return result;
    }
}
