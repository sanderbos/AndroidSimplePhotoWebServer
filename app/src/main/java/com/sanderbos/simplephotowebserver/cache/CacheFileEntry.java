package com.sanderbos.simplephotowebserver.cache;

import java.io.File;

/**
 * Representation of a file in the cache.
 */
public class CacheFileEntry {

    /**
     * The full path of the file being cached
     */
    private String path;

    /**
     * The last modification timestamp of the cached file.
     */
    private long lastModificationTimestamp;

    /**
     * The full path to the thumbnail of this file, if available (null otherwise).
     */
    private String thumbnailPath;

    /**
     * Constructor.
     * @param file The file being cached.
     */
    public CacheFileEntry(File file) {
        this.path = file.getAbsolutePath();
        this.lastModificationTimestamp = file.lastModified();
        this.thumbnailPath = file.getAbsolutePath();
    }

    /**
     * Get the full path of the file.
     * @return The full path of the file.
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the last modification date of the cached file.
     * @return The last modification date of the file.
     */
    public long getLastModificationTimestamp() {
        return lastModificationTimestamp;
    }

    /**
     * Get the path to the thumbnail, if available.
     * @return The full path to the thumbnail of this media file, or null in case no such thumbnail
     * is available.
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }
}
