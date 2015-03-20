package com.sanderbos.simplephotowebserver;

import android.app.Activity;

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
     * Url photo thumbnail action name (that displays an binary image thumbnail).
     */
    public static final String ACTION_URL_SHOW_THUMBNAIL = "/showThumbnail";


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
        addHtmlContent("<ul>");
        createDirectoryTreeRecursive(cachedDirectoryEntry, directoryEntryToHighlight);
        addHtmlContent("</ul>");
    }

    /**
     * Generate a directory structure HTML tree. This is a recursive method, in case there are
     * sub directories a tree is created for those as well.
     *
     * @param cachedDirectoryEntry      The directory to add an item for.
     * @param directoryEntryToHighlight The directory entry to highlight in the tree.
     */
    private void createDirectoryTreeRecursive(CacheDirectoryEntry cachedDirectoryEntry, CacheDirectoryEntry directoryEntryToHighlight) {
        String name = cachedDirectoryEntry.getName();
        String path = cachedDirectoryEntry.getFullPath();
        String url = constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, path);
        addHtmlContent("<li>");
        String hyperlink = createHyperLink(name, url, null);
        if (cachedDirectoryEntry.equals(directoryEntryToHighlight)) {
            hyperlink = "<b>" + hyperlink + "</b>";
        }
        addHtmlContent(hyperlink);
        addHtmlContent("</li>");
        List<CacheDirectoryEntry> subDirectories = cachedDirectoryEntry.getSubDirectoryList();
        if (subDirectories.size() > 0) {
            addHtmlContent("<ul>");
            for (CacheDirectoryEntry subDirectoryEntry : subDirectories) {
                createDirectoryTreeRecursive(subDirectoryEntry, directoryEntryToHighlight);
            }
            addHtmlContent("</ul>");
        }
    }

    /**
     * List the directory contents, as a set of thumbnails.
     *
     * @param cachedDirectoryEntry The directory whose content to show.
     * @param requestState         The request being displayed.
     */
    public void addDirectoryContentsAsThumbnails(CacheDirectoryEntry cachedDirectoryEntry, MediaRequestState requestState) {
        List<CacheFileEntry> fileEntries = cachedDirectoryEntry.getFileList();
        int thumbnailPageNumber = requestState.getCurrentThumbnailPage();
        if (thumbnailPageNumber < 0) {
            thumbnailPageNumber = 0;
        }

        addHtmlContent("<table><tr><td>");
        if (thumbnailPageNumber > 0) {
            addHtmlContent(createHyperLink("&lt;", constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, cachedDirectoryEntry.getFullPath(), thumbnailPageNumber - 1), null));
        }
        addHtmlContent("</td><td>");

        int firstItem = thumbnailPageNumber * THUMBNAIL_PAGE_SIZE;
        int lastItem = (thumbnailPageNumber + 1) * THUMBNAIL_PAGE_SIZE;
        int index;
        int currentColumnIndex = 0;

        addHtmlContent("<table><tr>");
        // This always renders an entire table
        for (index = firstItem; index < lastItem; index++) {

            if (currentColumnIndex > NUM_THUMBNAIL_COLUMNS) {
                // Start a new row
                currentColumnIndex = 0;
                addHtmlContent("</tr><tr>");
            }
            currentColumnIndex++;

            if (index < fileEntries.size()) {
                CacheFileEntry fileEntry = fileEntries.get(index);
                boolean isSelectedImage = fileEntry.getFullPath().equals((requestState.getCurrentImagePath()));
                addThumbnailHtml(fileEntry, isSelectedImage);
            }
        }
        addHtmlContent("</tr></table>");

        addHtmlContent("</td><td>");
        if (index < fileEntries.size()) {
            // There are more entries beyond the ones now shown.
            addHtmlContent(createHyperLink("&gt;", constructTargetURL(ACTION_URL_SHOW_DIRECTORY_PAGE, cachedDirectoryEntry.getFullPath(), thumbnailPageNumber + 1), null));
        }
        addHtmlContent("</td></tr></table>");
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
            addHtmlContent(createHyperLink("&lt;", constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, previousImagePath), null));
        }
        addHtmlContent("</td><td>");
        String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_PHOTO, imagePath), "image-main-regular", null);
        addHtmlContent(imageTag);
        addHtmlContent("</td><td>");
        if (nextImagePath != null) {
            addHtmlContent(createHyperLink("&gt;", constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, nextImagePath), null));
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
        String cellCssClass;
        if (isSelectedImage) {
            cellCssClass = "thumbnail-cell-selected";
        } else {
            cellCssClass = "thumbnail-cell-regular";
        }
        addHtmlContent("<td class=\"" + cellCssClass + "\">");
        String imageTag = createImage(constructTargetURL(ACTION_URL_SHOW_THUMBNAIL, fileEntry.getThumbnailPath()), "image-thumbnail", null);
        String imageTagWithHyperLink = createHyperLink(imageTag, constructTargetURL(ACTION_URL_SHOW_PHOTO_PAGE, fileEntry.getFullPath()), null);
        addHtmlContent(imageTagWithHyperLink);
        addHtmlContent("</td>");
    }

    /**
     * Construct an action URL string, of the format action?path=pathValue
     *
     * @param action             The base action URL
     * @param pathParameterValue The value to use for the path parameter.
     * @return The constructed string.
     */
    private String constructTargetURL(String action, String pathParameterValue) {
        return MessageFormat.format("{0}?{1}={2}", action, PARAMETER_PATH, pathParameterValue);
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
     * @param sourceURL The url to use in the source.
     * @param cssClass  Optional css class for use with the image tag.
     * @param altText   Optional alt attribute text.
     * @return The text of an image.
     */
    private String createImage(String sourceURL, String cssClass, String altText) {
        String result = MessageFormat.format("<img src=\"{0}\"", sourceURL);
        if (cssClass != null) {
            result += MessageFormat.format(" class=\"{0}\"", cssClass);
        }
        if (altText != null) {
            result += MessageFormat.format(" alt=\"{0}\"", altText);
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
     * Add a break newline to the HTML content.
     */
    public void addHtmlNewline() {
        addHtmlContent("<br/>");
    }

    /**
     * Add text to the main HTML content.
     *
     * @param contentFragment The content to add.
     */
    private void addHtmlContent(String contentFragment) {
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

}
