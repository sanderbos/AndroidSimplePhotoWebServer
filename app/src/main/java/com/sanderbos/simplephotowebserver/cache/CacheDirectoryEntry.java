package com.sanderbos.simplephotowebserver.cache;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representation of a directory in the cache.
 */
public class CacheDirectoryEntry {

    /**
     * List of file extensions that are accepted as media files.
     */
    private static final Set<String> KNOWN_MEDIA_EXTENSIONS = new HashSet<>();

    static {
        KNOWN_MEDIA_EXTENSIONS.add("jpg");
        KNOWN_MEDIA_EXTENSIONS.add("png");
        KNOWN_MEDIA_EXTENSIONS.add("gif");
    }

    /**
     * The full path of the cache directory being represented.
     */
    private String path;

    /**
     * The directory name.
     */
    private String name;

    /**
     * The list of subdirectories of this directory.
     */
    private List<CacheDirectoryEntry> subDirectoryList;

    /**
     * All cached directory entries share the same map of all found cached directories.
     */
    private Map<File, CacheDirectoryEntry> cachedDirectories;

    /**
     * The list of media files in this directory.
     */
    private List<CacheFileEntry> fileList;

    /**
     * Constructor for a new cached directory entry.
     *
     * @param directory         The directory file to represent in this object.
     * @param cachedDirectories The shared map of all cached directories.
     */
    public CacheDirectoryEntry(File directory, Map<File, CacheDirectoryEntry> cachedDirectories) {
        this.path = directory.getAbsolutePath();
        this.name = directory.getName();
        this.cachedDirectories = cachedDirectories;

        // This is the logical place to do the registration, to ensure it happens early
        // and it only has to happen once.
        cachedDirectories.put(directory, this);

        initializeSubdirectories();
    }

    /**
     * Recursively go through all sub-directories of this directory, and create the tree of
     * cached subdirectories.
     */
    private void initializeSubdirectories() {

        if (subDirectoryList == null) {
            subDirectoryList = new ArrayList<>();
            File directory = new File(path);
            File[] subDirectories = directory.listFiles(new DirectoryFilter());
            for (File subDirectory : subDirectories) {
                CacheDirectoryEntry subDirectoryEntry;
                if (!cachedDirectories.containsKey(subDirectory)) {
                    subDirectoryEntry = new CacheDirectoryEntry(subDirectory, cachedDirectories);
                } else {
                    // Sub directory already cached, but still add it to the list here.
                    subDirectoryEntry = cachedDirectories.get(subDirectory);
                }
                subDirectoryList.add(subDirectoryEntry);
            }
            Collections.sort(subDirectoryList, new DirectoryNameComparator());
        }
    }

    /**
     * Set up the cached file list of this directory, in case it has not been initialized yet.
     */
    public void initializeFileList() {
        if (fileList == null) {
            fileList = new ArrayList<>();
            File directory = new File(path);
            File[] files = directory.listFiles(new MediaFileFilter());
            for (File file : files) {
                if (file.isFile() && hasKnownExtension(file)) {
                    fileList.add(new CacheFileEntry(file));
                }
            }
            Collections.sort(fileList, new ModificationDateComparator());
        }
    }

    /**
     * Get the directory name.
     *
     * @return The name of the directory.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the directory path.
     *
     * @return The path of the directory.
     */
    public String getFullPath() {
        return path;
    }

    /**
     * Get the list of sub directories.
     * @return The list of sub directories (always initialized).
     */
    public List<CacheDirectoryEntry> getSubDirectoryList() {
        initializeSubdirectories();
        return subDirectoryList;
    }

    /**
     * Get the list of files in this directory (will initialize this list if this has not occurred yet.
     * @return The list of media files in this directory.
     */
    public List<CacheFileEntry> getFileList() {
        initializeFileList();
        return fileList;
    }

    /**
     * Check whether a file extension is among the recognized media files.
     *
     * @param file The file to check.
     * @return True in case the file extension is of a supported media type, false otherwise.
     */
    private static boolean hasKnownExtension(File file) {
        boolean result = false;
        int dotLocation = file.getName().lastIndexOf('.');
        if (dotLocation != -1) {
            String extension = file.getName().substring(dotLocation + 1).toLowerCase();
            if (KNOWN_MEDIA_EXTENSIONS.contains(extension)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * File filter that only accepts files with media extensions.
     */
    private static class MediaFileFilter implements FileFilter {

        /**
         * Implementation of file filter interface.
         *
         * @param file The file being filtered.
         * @return true in case the file should be considered a media file.
         */
        @Override
        public boolean accept(File file) {
            return (file.isFile() && hasKnownExtension(file));
        }
    }

    /**
     * File filter that only accepts directories.
     */
    private static class DirectoryFilter implements FileFilter {

        /**
         * Implementation of file filter interface.
         *
         * @param file The file being filtered.
         * @return true in case the file is a directory.
         */
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    /**
     * Comparator implementation for CacheFileEntry, based on last modification timestamp.
     */
    private class ModificationDateComparator implements java.util.Comparator<CacheFileEntry> {
        /**
         * Implementation of compare, based on last modification timestamp.
         *
         * @param first  The first object to compare.
         * @param second The second object to compare.
         * @return The comparison result.
         */
        @Override
        public int compare(CacheFileEntry first, CacheFileEntry second) {

            long timeDifference = (first.getLastModificationTimestamp() - second.getLastModificationTimestamp());
            int result;
            if (timeDifference < 0) {
                result = -1;
            } else if (timeDifference > 0) {
                result = 1;
            } else {
                result = 0;
            }
            return result;
        }
    }

    /**
     * Comparator that compares cached directory entries based on their name.
     */
    private class DirectoryNameComparator implements java.util.Comparator<CacheDirectoryEntry> {
        /**
         * Implementation of compare method.
         *
         * @param first  The first cached directory item to compare.
         * @param second The second cached directory item to compare.
         * @return The comparison result.
         */
        @Override
        public int compare(CacheDirectoryEntry first, CacheDirectoryEntry second) {
            return first.getName().compareTo(second.getName());
        }
    }

    /**
     * Implementation of equals.
     * @param o Other object.
     * @return True in case the other object represents the same directory, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheDirectoryEntry that = (CacheDirectoryEntry) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;

        return true;
    }

    /**
     * Implementation of hashCode.
     * @return The hashcode of the object, based on name and path.
     */
    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
