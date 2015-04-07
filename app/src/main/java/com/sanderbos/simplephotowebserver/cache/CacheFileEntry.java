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
     * Has the media database queried for a thumbnail for this image?
     */
    private boolean checkedForMediaStoreThumbnail = false;

    /**
     * Constructor.
     * @param file The file being cached.
     * @param cache A shared registry of all cached directories and files.
     */
    public CacheFileEntry(File file, CacheRegistry cache) {
        this.path = file.getAbsolutePath();
        this.lastModificationTimestamp = file.lastModified();
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

    /**
     * Set the thumbnail path (should be a reference to an existing thumbnail for the image this
     * cache file entry represents).
     * @param thumbnailPath The path of the thumbnail.
     */
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * Determine whether the media store has ever been checked for a thumbnail for this image.
     * @return True in case the checkedForMediaStoreThumbnail has been set, false otherwise.
     */
    public boolean isCheckedForMediaStoreThumbnail() {
        return checkedForMediaStoreThumbnail;
    }

    /**
     * Call this method (with true argument) in case it should be registered the media store
     * has been checked for a thumbnail.
     * @param checkedForMediaThumbnail The new value for whether the thumbnail media has been
     *                                 determined.
     */
    public void setCheckedForMediaStoreThumbnail(boolean checkedForMediaThumbnail) {
        this.checkedForMediaStoreThumbnail = checkedForMediaThumbnail;
    }
}
