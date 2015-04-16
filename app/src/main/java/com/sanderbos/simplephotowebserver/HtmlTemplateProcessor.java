package com.sanderbos.simplephotowebserver;

import android.app.Activity;
import android.text.TextUtils;

import com.sanderbos.simplephotowebserver.cache.CacheDirectoryEntry;
import com.sanderbos.simplephotowebserver.cache.CacheFileEntry;

import java.text.MessageFormat;
import java.util.List;


/**
 * String based simple HTML templating, constructing a string containing a HTML document.
 * Uses string resources for templating (stored in values.html_fragments.xml).
 * Performs string replacement based on ||keyword||.
 */
public class HtmlTemplateProcessor {

    /**
     * Url path parameter name.
     */
    public static final String PARAMETER_PATH = "path";

    /**
     * Page number parameter name.
     */
    public static final String PARAMETER_PAGE_NUMBER = "page";

    /**
     * Parameter used to set that the directory structure should be made visible.
     */
    public static final String PARAMETER_FORCE_SHOW_DIRECTORY = "forceShowDirStructure";

    /**
     * Url directory page parameter name.
     */
    public static final String ACTION_URL_SHOW_DIRECTORY_PAGE = "/showDirectoryPage";

    /**
     * Url photo page action name.
     */
    public static final String ACTION_URL_SHOW_PHOTO_PAGE = "/showPhotoPage";

    /**
     * Url photo action name (that displays a binary photo image).
     */
    public static final String ACTION_URL_SHOW_PHOTO = "/showPhoto";

    /**
     * Url photo action name (that returns a binary photo image as an octal stream).
     */
    public static final String ACTION_URL_DOWNLOAD_FILE = "/downloadPhoto";

    /**
     * Url photo thumbnail action name (that displays an binary image thumbnail).
     */
    public static final String ACTION_URL_SHOW_THUMBNAIL = "/showThumbnail";

    /**
     * Url photo action name (that displays an in-app icon).
     */
    public static final String ACTION_URL_SHOW_ICON = "/showIcon";

    /**
     * Keywords in the templates have the format ||keyword||
     */
    private static final String SEPARATOR = "||";

    /**
     * Template keyword for titles.
     */
    private static final String KEYWORD_TITLE = "title";

    /**
     * Template keyword for content.
     */
    private static final String KEYWORD_CONTENT = "content";

    /**
     * Template keyword for uri.
     */
    private static final String KEYWORD_URI = "uri";

    /**
     * The number of thumbnail rows.
     */
    private static final int NUM_THUMBNAIL_ROWS = 2;

    /**
     * The number of thumbnail columns.
     */
    private static final int NUM_THUMBNAIL_COLUMNS = 10;

    /**
     * Page size (for thumbnails).
     */
    public static final int THUMBNAIL_PAGE_SIZE = NUM_THUMBNAIL_ROWS * NUM_THUMBNAIL_COLUMNS;

    /**
     * This thummbnail size should match the width specified in html_fragments.xml image-thumbnail css class.
     */
    public static final int THUMBNAIL_WIDTH = 40;

    /**
     * A context object for the template.
     */
    private Activity context;

    /**
     * The running content of the template, stored without a header and footer.
     */
    private StringBuilder content;

    /**
     * The title to use in the HTML document.
     */
    private String title = "";

    /**
     * Constructor, starts a new empty document.
     *
     * @param context Context for the HtmlTemplate object, used amongst other things to resolve string resources.
     */
    public HtmlTemplateProcessor(Activity context) {
        this.content = new StringBuilder();
        this.context = context;

        this.title = getResourceText(R.string.title_regular);
    }

    /**
     * Get the title set on this HtmlTemplate object.
     *
     * @return The current title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title on this HtmlTemplate object.
     *
     * @param title The new title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Set a 404 title based on a URI
     *
     * @param uri The uri to use in the 404 title.
     */
    public void set404Title(String uri) {
        String titleTemplate = getResourceText(R.string.title_404);
        setTitle(replaceTemplateVariable(titleTemplate, KEYWORD_URI, uri));
    }

    /**
     * Set a 500 title based on a URI
     *
     * @param uri The uri to use in the 500 title.
     */
    public void set500Title(String uri) {
        String titleTemplate = getResourceText(R.string.title_500);
        setTitle(replaceTemplateVariable(titleTemplate, KEYWORD_URI, uri));
    }

    /**
     * Get the html templating output.
     *
     * @return A string representing an HTML document.
     */
    public String getHtmlOutput() {

        String result = getResourceText(R.string.html_main_template);
        result = replaceTemplateVariable(result, KEYWORD_TITLE, getTitle());
        result = replaceTemplateVariable(result, KEYWORD_CONTENT, content.toString());
        return result;
    }

    /**
     * Generate a directory structure HTML tree.
     *
     * @param cachedDirectoryEntry      The root directory to generate a tree for.
     * @param directoryEntryToHighlight The directory entry to highlight in the tree.
     */
    public void createDirectoryTree(CacheDirectoryEntry cachedDirectoryEntry, CacheDirectoryEntry directoryEntryToHighlight) {
        addHtmlContent(createDirectoryTreeRecursive(cachedDirectoryEntry, directoryEntryToHighlight).toString());
    }

    /**
     * Generate a directory structure HTML tree. This is a recursive method, in case there are
     * sub directories a tree is created for those as well.
     *
     * @param cachedDirectoryEntry      The directory to add an item for.
     * @param directoryEntryToHighlight The directory entry to highlight in the tree.
     * @return The constructed html content in this method.
     */
    private StringBuilder createDirectoryTreeRecursive(CacheDirectoryEntry cachedDirectoryEntry, CacheDirectoryEntry directoryEntryToHighlight) {
        // Check sublist first, even though content is added later.
        StringBuilder subContentString = new StringBuilder();
        List<CacheDirectoryEntry> subDirectories = cachedDirectoryEntry.getSubDirectoryList();
        if (subDirectories.size() > 0) {
            StringBuilder subContent = new StringBuilder();
            for (CacheDirectoryEntry subDirectoryEntry : subDirectories) {
                subContent.append(createDirectoryTreeRecursive(subDirectoryEntry, directoryEntryToHighlight));

            }
            if (subContent.length() > 0) {
                subContentString.append("<ul>");
                subContentString.append(subContent);
                subContentString.append("</ul>");
            }
        }

        StringBuilder entryString = new StringBuilder();
        int fileCount = cachedDirectoryEntry.getMediaFilesCount();
        String directoryName = cachedDirectoryEntry.getName();
        if (fileCount > 0) {
            String path = cachedDirectoryEntry.getFullPath();
            String url = constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, path);
            entryString.append("<li>");
            String hyperlink = createHyperLink(directoryName, url, null);
            if (cachedDirectoryEntry.equals(directoryEntryToHighlight)) {
                hyperlink = "<b>" + hyperlink + "</b>";
            }
            entryString.append(hyperlink);
            String fileCountString = MessageFormat.format(" ({0}) ", fileCount);
            entryString.append(fileCountString);
            entryString.append("</li>");
        } else if (subContentString.length() > 0) {
            // Still need a list item entry, for the subtree
            entryString.append("<li>");
            entryString.append(directoryName);
            entryString.append("</li>");
        }

        StringBuilder resultString = new StringBuilder();
        resultString.append(entryString);
        resultString.append(subContentString);
        return resultString;
    }

    /**
     * List the directory contents, as a set of thumbnails.
     *
     * @param cachedDirectoryEntry The directory whose content to show.
     * @param requestState         The request being displayed.
     * @return For convenience, return the full image path of the first
     * thumbnail added (otherwise logic to get that that would have to be duplicated).
     */
    public String addDirectoryContentsAsThumbnails(CacheDirectoryEntry cachedDirectoryEntry, MediaRequestState requestState) {
        String selectedImagePath = requestState.getCurrentImagePath();

        List<CacheFileEntry> fileEntries = cachedDirectoryEntry.getFileList();
        int thumbnailPageNumber = requestState.getCurrentThumbnailPage();
        if (thumbnailPageNumber < 0) {
            thumbnailPageNumber = 0;
        }

        addHtmlContent("<table><tr><td>");
        if (thumbnailPageNumber > 0) {
            String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_ICON, "previous"), "", getResourceText(R.string.html_text_previous_page));
            addHtmlContent(createHyperLink(imageTag, constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, cachedDirectoryEntry.getFullPath(), thumbnailPageNumber - 1), null));
        }
        addHtmlContent("</td><td>");

        int firstItem = thumbnailPageNumber * THUMBNAIL_PAGE_SIZE;
        int lastItem = (thumbnailPageNumber + 1) * THUMBNAIL_PAGE_SIZE;
        int index;
        int currentColumnIndex = 0;

        addHtmlContent("<div class='thumbnail-table-div'>");
        addHtmlContent("<table><tr>");
        // This always renders an entire table
        for (index = firstItem; index < lastItem; index++) {

            if (currentColumnIndex >= NUM_THUMBNAIL_COLUMNS) {
                // Start a new row
                currentColumnIndex = 0;
                addHtmlContent("</tr><tr>");
            }
            currentColumnIndex++;

            if (index < fileEntries.size()) {
                CacheFileEntry fileEntry = fileEntries.get(index);

                // This is logic that will make the selected image the first thumbnail in
                // case there is no selected image yet.
                if (selectedImagePath == null) {
                    selectedImagePath = fileEntry.getFullPath();
                }

                boolean isSelectedImage = fileEntry.getFullPath().equals(selectedImagePath);
                addThumbnailHtml(fileEntry, isSelectedImage);
            }
        }
        addHtmlContent("</tr></table>");
        addHtmlContent("</div>");

        addHtmlContent("</td><td>");
        if (index < fileEntries.size()) {
            // There are more entries beyond the ones now shown.
            String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_ICON, "next"), "", getResourceText(R.string.html_text_next_page));
            addHtmlContent(createHyperLink(imageTag, constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, cachedDirectoryEntry.getFullPath(), thumbnailPageNumber + 1), null));
        }
        addHtmlContent("</td></tr></table>");

        return selectedImagePath;
    }

    /**
     * Add the image HTML for an image.
     *
     * @param imagePath         The full path of an image to show.
     * @param previousImagePath The full path of a previous image in the list, if such an image exists.
     * @param nextImagePath     The full path of a next image in the list, if such an image exists.
     */
    public void addMainImageHtml(String imagePath, String previousImagePath, String nextImagePath) {
        addHtmlContent("<table><tr><td>");
        if (previousImagePath != null) {
            String previousImageTag = createImage(constructTargetURL(ACTION_URL_SHOW_ICON, "previous"), "", getResourceText(R.string.html_text_previous_image));
            addHtmlContent(createHyperLink(previousImageTag, constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, previousImagePath), null));
        }
        addHtmlContent("</td><td>");

        String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_PHOTO, imagePath), "image-main-regular", null);
        addHtmlContent(imageTag);
        String downloadTag = createImage(constructTargetURL(ACTION_URL_SHOW_ICON, "download"), "image-download-icon", getResourceText(R.string.html_text_download));
        addHtmlContent("<br/>");
        addHtmlContent(createHyperLink(downloadTag, constructDownloadURL(ACTION_URL_DOWNLOAD_FILE, imagePath), null));

        addHtmlContent("</td><td>");
        if (nextImagePath != null) {
            String nextImageTag = createImage(constructTargetURL(ACTION_URL_SHOW_ICON, "next"), "", getResourceText(R.string.html_text_next_image));
            addHtmlContent(createHyperLink(nextImageTag, constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, nextImagePath), null));
        }
        addHtmlContent("</td></tr></table>");
    }

    /**
     * Create and add the HTML for one thumbnail cell.
     *
     * @param fileEntry       The file entry to show the thumbail for.
     * @param isSelectedImage Whether this image is the currently selected image or not.
     */
    private void addThumbnailHtml(CacheFileEntry fileEntry, boolean isSelectedImage) {
        String cellCssClass = "thumbnail-cell-regular";
        if (isSelectedImage) {
            cellCssClass += " image-thumbnail-selected";
        }
        addHtmlContent("<td class=\"" + cellCssClass + "\">");
        if (isSelectedImage) {
            addHtmlContent("<div class='thumbnail-cell-div'>");
        } else {
            addHtmlContent("<div class='thumbnail-cell-div'>");
        }
        String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_THUMBNAIL, fileEntry.getFullPath()), "image-thumbnail", null);
        String imageTagWithHyperLink = createHyperLink(imageTag, constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, fileEntry.getFullPath()), null);
        addHtmlContent(imageTagWithHyperLink);
        addHtmlContent("</div>");
        addHtmlContent("</td>");
    }

    /**
     * Construct an action URL string, of the format action?path=pathParameterValue
     *
     * @param action             The base action URL
     * @param pathParameterValue The value to use for the path parameter.
     * @return The constructed string.
     */
    private String constructTargetURL(String action, String pathParameterValue) {
        return MessageFormat.format("{0}?{1}={2}", action, PARAMETER_PATH, pathParameterValue);
    }

    /**
     * Construct an URL string, of the format actionpathParameterValue. Because the filename
     * is based on the url part excluding the query string, the file argument must not be in
     * a query string.
     *
     * @param action             The base action URL
     * @param pathParameterValue The path value to use in the URL.
     * @return The constructed string.
     */
    private String constructDownloadURL(String action, String pathParameterValue) {
        // No need to add extra /, as path parameter is already absolute (and even if it would
        // not start with a '/' it would still work.
        return MessageFormat.format("{0}{1}", action, pathParameterValue);
    }

    /**
     * Construct an action URL string, of the format action?path=pathValue
     *
     * @param action              The base action URL
     * @param pathParameterValue  The value to use for the path parameter.
     * @param thumbnailPageNumber The thumbnail page number to include in the constructed URL
     * @return The constructed string.
     */
    private String constructTargetURL(String action, String pathParameterValue, int thumbnailPageNumber) {
        return MessageFormat.format("{0}?{3}={4}&{1}={2}", action, PARAMETER_PATH, pathParameterValue,
                PARAMETER_PAGE_NUMBER, String.valueOf(thumbnailPageNumber));
    }

    /**
     * Create a hyperlink string.
     *
     * @param linkContent The text or html fragment to place within the hyperlink.
     * @param url         The URL to display.
     * @param altText     The alt-text to use, if relevant.
     */
    private String createHyperLink(String linkContent, String url, String altText) {
        String htmlHyperLink;
        if (altText != null) {
            htmlHyperLink = MessageFormat.format("<a href=\"{1}\" alt=\"{2}\">{0}</a>", linkContent, url, altText);
        } else {
            htmlHyperLink = MessageFormat.format("<a href=\"{1}\">{0}</a>", linkContent, url);
        }
        return htmlHyperLink;
    }

    /**
     * Construct an img tag.
     *
     * @param sourceURL   The url to use in the source.
     * @param cssClass    Optional css class for use with the image tag.
     * @param toolTipText Optional tooltip (title) attribute text.
     * @return The text of an image.
     */
    private String createImage(String sourceURL, String cssClass, String toolTipText) {
        String result = MessageFormat.format("<img src=\"{0}\"", sourceURL);
        if (!TextUtils.isEmpty(cssClass)) {
            result += MessageFormat.format(" class=\"{0}\"", cssClass);
        }
        if (!TextUtils.isEmpty(toolTipText)) {
            result += MessageFormat.format(" title=\"{0}\"", toolTipText);
        }
        result += "/>";
        return result;
    }

    /**
     * Add a separator to the content.
     */
    public void addSeparator() {
        addHtmlContent(getResourceText(R.string.html_separator));
    }

    /**
     * Add text to the main HTML content.
     *
     * @param contentFragment The content to add.
     */
    public void addHtmlContent(String contentFragment) {
        content.append(contentFragment);
        content.append("\n");
    }

    /**
     * Utility method to get a resource string text.
     *
     * @param resourceId The resource to resolve (this should be a valid identifier).
     * @return The resolved string (this method does not expect an error to occur).
     */
    private String getResourceText(int resourceId) {
        return context.getResources().getText(resourceId).toString();
    }

    /**
     * Perform the basic template replacement, replacing ||keyword|| by a value in a template string.
     *
     * @param template         The template string to perform the replacement on.
     * @param keyword          The keyword to replace.
     * @param replacementValue The replacement value (which is not evaluated further).
     * @return The template, with replacement executed.
     */
    private String replaceTemplateVariable(String template, String keyword, String replacementValue) {
        if (replacementValue == null) {
            replacementValue = "";
        }
        String textToLookup = SEPARATOR + keyword + SEPARATOR;
        return template.replace(textToLookup, replacementValue);
    }

    /**
     * Create a toggle hyperlink for the directory structure. Based on the current request, make
     * that an expand or collapse icon.
     *
     * @param currentDisplayState Information about the current http request being processed, used
     *                            to create a hyperlink to the current page in the toggle.
     */
    public void addDirectoryContentToggle(MediaRequestState currentDisplayState) {
        // TODO: This method is now 'not ideal'. perhaps revisit later.
        if (currentDisplayState == null) {
            currentDisplayState = new MediaRequestState();
        }
        StringBuilder hyperLinkURL = new StringBuilder();
        String currentPath;
        if (currentDisplayState.getCurrentImagePath() == null) {
            hyperLinkURL.append(ACTION_URL_SHOW_DIRECTORY_PAGE);
            currentPath = currentDisplayState.getCurrentDirectoryPath();
        } else {
            hyperLinkURL.append(ACTION_URL_SHOW_PHOTO_PAGE);
            currentPath = currentDisplayState.getCurrentImagePath();
        }
        // Hack to prevent making the rest of this method even more unreadable
        hyperLinkURL.append("?dummy=dummy");
        if (currentPath != null) {
            hyperLinkURL.append("&" + PARAMETER_PATH + "=" + currentPath);
        }
        if (currentDisplayState.getCurrentThumbnailPage() != -1) {
            hyperLinkURL.append("&" + PARAMETER_PAGE_NUMBER + "=" + String.valueOf(currentDisplayState.getCurrentThumbnailPage()));
        }

        String imageFile;
        String altText;
        if (currentDisplayState.getCurrentDirectoryPath() == null || currentDisplayState.isForceShowDirectoryStructure()) {
            imageFile = "/showIcon?path=collapse";
            altText = getResourceText(R.string.html_text_collapse_folder_selection);
        } else {
            imageFile = "/showIcon?path=expand";
            altText = getResourceText(R.string.html_text_expand_folder_selection);
            hyperLinkURL.append("&" + PARAMETER_FORCE_SHOW_DIRECTORY + "=true");
        }
        addHtmlContent(createHyperLink(createImage(imageFile, "", altText), hyperLinkURL.toString(), ""));
    }

}
