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
     * Whether a special parameter is set to force showing the directory structure.
     */
    private boolean forceShowDirectoryStructure;

    /**
     * Whether or not the request state is for full screen mode.
     */
    private boolean inFullscreenMode;

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

    /**
     * Set whether the directory structure should be shown.
     * @param forceShowDirectoryStructure Whether or not the directory structure should be shown.
     */
    public void setForceShowDirectoryStructure(boolean forceShowDirectoryStructure) {
        this.forceShowDirectoryStructure = forceShowDirectoryStructure;
    }

    /**
     * Get whether the directory structure should be shown, even in case a photo is selected.
     * @return True in case the directory structure should be shown (false is default case).
     */
    public boolean isForceShowDirectoryStructure() {
        return this.forceShowDirectoryStructure;
    }

    /**
     * Whether or not the request state is in full screen mode.
     * @return True in case the media request is for full screen mode.
     */
    public boolean isInFullscreenMode() {
        return inFullscreenMode;
    }

    /**
     * Set whether or not the request state is in full screen mode.
     * @param  inFullscreenMode The new value for full screen mode.
     */
    public void setInFullscreenMode(boolean inFullscreenMode) {
        this.inFullscreenMode = inFullscreenMode;
    }
}
