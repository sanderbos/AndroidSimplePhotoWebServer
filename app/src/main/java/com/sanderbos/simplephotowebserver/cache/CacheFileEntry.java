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
     * All cached entries share the same cache registry.
     */
    private CacheRegistry cache;

    /**
     * Constructor.
     * @param file The file being cached.
     * @param cache A shared registry of all cached directories and files.
     */
    public CacheFileEntry(File file, CacheRegistry cache) {
        this.path = file.getAbsolutePath();
        this.lastModificationTimestamp = file.lastModified();
        this.thumbnailPath = file.getAbsolutePath();
        this.cache = cache;

        this.cache.registerFile(this);
    }

    /**
     * Get the full path of the file.
     * @return The full path of the file.
     */
    public String getFullPath() {
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
