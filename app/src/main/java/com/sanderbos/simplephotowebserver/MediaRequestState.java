package com.sanderbos.simplephotowebserver;

/**
 * Simple structure to store the state extracted from the HTML request, representing the photo information data being shown right now.
 */
public class MediaRequestState {
    /**
     * The current directory being displayed (should always be filled in).
     */
    private String currentDirectoryPath;

    /**
     * The current picture being displayed (if null, no specific picture currently selected).
     */
    private String currentImagePath = null;

    /**
     * The current thumbnail path (if not explicitly set, use the first page).
     */
    private int currentThumbnailPage = -1;


    /**
     * Get current directory path.
     * @return The current directory path.
     */
    public String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }

    /**
     * Set current directory path.
     * @param currentDirectoryPath The current directory path.
     */
    public void setCurrentDirectoryPath(String currentDirectoryPath) {
        this.currentDirectoryPath = currentDirectoryPath;
    }

    /**
     * Get current image path.
     * @return The current image path.
     */
    public String getCurrentImagePath() {
        return currentImagePath;
    }

    /**
     * Set current image path.
     * @param currentImagePath The current image path.
     */
    public void setCurrentImagePath(String currentImagePath) {
        this.currentImagePath = currentImagePath;
    }

    /**
     * Get current thumbnail page.
     * @return The current thumbnail page.
     */
    public int getCurrentThumbnailPage() {
        return currentThumbnailPage;
    }


    /**
     * Set current thumbnail page.
     * @param  currentThumbnailPage The current thumbnail page.
     */
    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        this.currentThumbnailPage = currentThumbnailPage;
    }
}
