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
    public static final String PATH_PARAM_NAME = "path";

    /**
     * Url directory parameter name.
     */
    public static final String ACTION_SHOW_DIR_NAME = "/showDir";

    /**
     * Url photo action name.
     */
    public static final String ACTION_SHOW_PHOTO_NAME = "/showPhoto";


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
     * Add a separator to the content.
     */
    public void addSeparator() {
        addStringContent(getResourceText(R.string.html_separator));
    }

    /**
     * Add a break newline to the HTML content.
     */
    public void addHtmlNewline() {
        addStringContent("<br/>");
    }

    /**
     * Generate a directory structure HTML tree.
     *
     * @param cachedDirectoryEntry      The root directory to generate a tree for.
     * @param directoryEntryToHighlight The directory entry to highlight in the tree.
     */
    public void createDirectoryTree(CacheDirectoryEntry cachedDirectoryEntry, CacheDirectoryEntry directoryEntryToHighlight) {
        addStringContent("<ul>");
        createDirectoryTreeRecursive(cachedDirectoryEntry, directoryEntryToHighlight);
        addStringContent("</ul>");
    }

    /**
     * Generate a directory structure HTML tree. This is a recursive method, in case there are
     * sub directories a tree is created for those as well.
     *
     * @param cachedDirectoryEntry The directory to add an item for.
     * @param directoryEntryToHighlight The directory entry to highlight in the tree.
     */
    private void createDirectoryTreeRecursive(CacheDirectoryEntry cachedDirectoryEntry, CacheDirectoryEntry directoryEntryToHighlight) {
        String name = cachedDirectoryEntry.getName();
        String path = cachedDirectoryEntry.getFullPath();
        String url = constructActionURL(ACTION_SHOW_DIR_NAME, path);
        addStringContent("<li>");
        String hyperlink = createHyperLink(name, url, null);
        if (cachedDirectoryEntry.equals(directoryEntryToHighlight)) {
            hyperlink = "<b>" + hyperlink + "</b>";
        }
        addStringContent(hyperlink);
        addStringContent("</li>");
        List<CacheDirectoryEntry> subDirectories = cachedDirectoryEntry.getSubDirectoryList();
        if (subDirectories.size() > 0) {
            addStringContent("<ul>");
            for (CacheDirectoryEntry subDirectoryEntry : subDirectories) {
                createDirectoryTreeRecursive(subDirectoryEntry, directoryEntryToHighlight);
            }
            addStringContent("</ul>");
        }
    }

    /**
     * Construct an action URL string, of the format action?path=pathValue
     *
     * @param action             The base action URL
     * @param pathParameterValue The value to use for the path parameter.
     * @return The constructed string.
     */
    private String constructActionURL(String action, String pathParameterValue) {
        return MessageFormat.format("{0}?{1}={2}", action, PATH_PARAM_NAME, pathParameterValue);
    }

    /**
     * Create a hyperlink string.
     *
     * @param name    The text to use in the hyperlink.
     * @param url     The URL to display.
     * @param altText The alt-text to use, if relevant.
     */
    private String createHyperLink(String name, String url, String altText) {
        String htmlHyperLink;
        if (altText != null) {
            htmlHyperLink = MessageFormat.format("<a href=\"{1}\" alt=\"{2}\">{0}</a>", name, url, altText);
        } else {
            htmlHyperLink = MessageFormat.format("<a href=\"{1}\">{0}</a>", name, url);
        }
        return htmlHyperLink;
    }

    /**
     * List the directory contents, as a set of thumbnails.
     *
     * @param cachedDirectoryEntry The directory whose content to show.
     */
    public void addDirectoryContentsAsThumbnails(CacheDirectoryEntry cachedDirectoryEntry) {
        List<CacheFileEntry> fileEntries = cachedDirectoryEntry.getFileList();
        for (CacheFileEntry fileEntry : fileEntries) {
            addStringContent(createHyperLink(fileEntry.getPath(), constructActionURL(ACTION_SHOW_PHOTO_NAME, fileEntry.getPath()), null));
            addHtmlNewline();
        }
    }

    /**
     * Add text to the main HTML content.
     *
     * @param contentFragment The content to add.
     */
    private void addStringContent(String contentFragment) {
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
