package com.sanderbos.simplephotowebserver.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache registry, with various ways to access the directory and file cache:
 * - By path.
 * An (one) instance of this object is meant to be shared by all the cached items.
 */
public class CacheRegistry {

    /**
     * All cached directory entries, stored by path.
     */
    private Map<String, CacheDirectoryEntry> cachedDirectories = new HashMap<>();

    /**
     * All cached file entries, stored by path.
     */
    private Map<String, CacheFileEntry> cachedFiles = new HashMap<>();

    /**
     * Public constructor.
     */
    public CacheRegistry() {

    }

    /**
     * Register a new directory in the cache.
     *
     * @param cacheDirectoryEntry The directory to cache.
     */
    public void registerDirectory(CacheDirectoryEntry cacheDirectoryEntry) {
        cachedDirectories.put(cacheDirectoryEntry.getFullPath(), cacheDirectoryEntry);
    }

    /**
     * Register a new file in the cache.
     *
     * @param cacheFileEntry The file to cache.
     */
    public void registerFile(CacheFileEntry cacheFileEntry) {
        cachedFiles.put(cacheFileEntry.getFullPath(), cacheFileEntry);
    }

    /**
     * Get a directory from the cache.
     *
     * @param path The path to the cached directory.
     * @return The cached entry, or null in case it does not exist.
     */
    public CacheDirectoryEntry getCachedDirectory(String path) {
        return cachedDirectories.get(path);
    }

    /**
     * Get a file from the cache.
     *
     * @param path The path to the cached file.
     * @return The cached entry, or null in case it does not exist.
     */
    public CacheFileEntry getCachedFile(String path) {
        return cachedFiles.get(path);
    }
}
