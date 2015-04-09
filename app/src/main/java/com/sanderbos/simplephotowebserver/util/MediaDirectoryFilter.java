package com.sanderbos.simplephotowebserver.util;

import java.io.File;
import java.io.FileFilter;

/**
 * File filter that only accepts directories, and prevents certain directories from being accepted.
 */
public class MediaDirectoryFilter implements FileFilter {

    /**
     * Implementation of file filter interface.
     *
     * @param file The file being filtered.
     * @return true in case the file is a directory.
     */
    @Override
    public boolean accept(File file) {
        boolean result = file.isDirectory();
        String fullPath = file.getAbsolutePath();
        // Fix practical bug, prevent wondering of into the data directory.
        if (fullPath.contains("/data/data") || fullPath.contains("/Android/data/")) {
            result = false;
        }
        // Exclude hidden directories
        if (file.getName().startsWith(".")) {
            result = false;
        }
        return result;
    }
}