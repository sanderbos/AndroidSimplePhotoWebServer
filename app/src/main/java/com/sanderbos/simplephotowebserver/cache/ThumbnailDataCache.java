package com.sanderbos.simplephotowebserver.cache;

import com.sanderbos.simplephotowebserver.util.ImageOrientation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple most recently used cache for thumbnail and rotated images and rotated images in byte array form.
 */
public class ThumbnailDataCache {

    /**
     * The default cache size (measured in bytes of actual thumbnail-data) kept.
     * (2 MB)
     */
    public static final int DEFAULT_CACHE_SIZE = 2 * 1024 * 1024;

    /**
     * The maximum size of byte-array data in this cache.
     */
    private int maximumCacheSize;

    /**
     * The current running total of cached byte-array data in this cache.
     */
    private int currentCacheSize;

    /**
     * Reference to the underlying LRU collection. Keys in this cache can be an image path
     * (for thumbnails) or 'imagePath|rotationInDegrees' (for images).
     */
    private LRUCache cache;

    /**
     * Constructor.
     *
     * @param sizeInBytes The maximum size of this cache in memory.
     */
    public ThumbnailDataCache(int sizeInBytes) {
        this.maximumCacheSize = sizeInBytes;
        this.currentCacheSize = 0;
        this.cache = new LRUCache();
    }

    /**
     * Get an item from the cache (if available).
     *
     * @param imagePath The path of the image (not the thumbnail) to get from the cache.
     * @return The thumbnail JPEG data of the thumbnail, or null in case that data is currently
     * not cached.
     */
    public synchronized byte[] getThumbnailFromCache(String imagePath) {
        byte[] result = null;
        if (this.cache.containsKey(imagePath)) {
            // This will update the LRU info
            result = this.cache.get(imagePath);
        }
        return result;
    }

    /**
     * Get an item from the cache (if available).
     *
     * @param imagePath The path of the image to get from the cache.
     * @param rotation  The rotation to get the image path for.
     * @return The rotated JPEG data of the image, or null in case that data is currently
     * not cached.
     */
    public byte[] getImageFromCache(String imagePath, ImageOrientation rotation) {
        return getThumbnailFromCache(createPathWithRotation(imagePath, rotation));
    }

    /**
     * Add an item to the cache (the cache may be shrunk during this operation).
     *
     * @param imagePath The path of the image for which thumbnail data is being added.
     * @param imageData The image data to cache.
     */
    public synchronized void addThumbnailToCache(String imagePath, byte[] imageData) {
        int extraDataSize = imageData.length;
        if (!this.cache.containsKey(imagePath) && extraDataSize < maximumCacheSize) {
            makeRoom(imageData.length);
            cache.put(imagePath, imageData);
            this.currentCacheSize += extraDataSize;
        }
    }

    /**
     * Add an item to the cache (the cache may be shrunk during this operation).
     *
     * @param imagePath The path of the image for which data is being added.
     * @param rotation  The rotation of the data cached.
     * @param imageData The image data to cache.
     */
    public void addImageToCache(String imagePath, ImageOrientation rotation, byte[] imageData) {
        addThumbnailToCache(createPathWithRotation(imagePath, rotation), imageData);
    }

    /**
     * Construct a string of the form 'path|rotation'
     *
     * @param path     The path to use in the constructed string.
     * @param rotation The rotation to use in the constructed string.
     * @return A string of the format 'path|rotationInDegrees'.
     */
    private String createPathWithRotation(String path, ImageOrientation rotation) {
        return path + String.valueOf(rotation.getRotationInDegrees());
    }

    /**
     * Check whether the extra item will fit in the cache, and make it smaller otherwise.
     *
     * @param extraDataSize The size of the extra item about to be added to the cache.
     */
    private void makeRoom(int extraDataSize) {
        if (extraDataSize >= maximumCacheSize) {
            // Check is already done in addThumbnailToCache
            throw new RuntimeException("Internal error, we should not cache these");
        }
        while (currentCacheSize + extraDataSize > maximumCacheSize) {
            // remove LRU item, which is first in the map (checked, keyset is also ordered)
            String lruPathKey = cache.keySet().iterator().next();
            this.currentCacheSize -= cache.get(lruPathKey).length;
            cache.remove(lruPathKey);
        }
    }

    /**
     * LRU cache (see http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/ and other places)
     */
    private class LRUCache extends LinkedHashMap<String, byte[]> {

        /**
         * Enable the LRU-mode of LinkedHashMap.
         */
        private static final boolean USE_ACCESS_ORDER = true;

        /**
         * Constructor, create a new LRU cache.
         */
        public LRUCache() {
            super(16, 0.75f, USE_ACCESS_ORDER);
        }

        /**
         * Method to override to make it into an LRU cache.
         *
         * @param eldest The item that is nominated to be removed if space is needed.
         * @return Whether or not action is needed.
         */
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            // This should never happen, as we check and remove items before adding anything.
            // (also, this method will be called at most once during addition so reaally cannot
            // be used)
            return currentCacheSize > maximumCacheSize;
        }
    }
}
