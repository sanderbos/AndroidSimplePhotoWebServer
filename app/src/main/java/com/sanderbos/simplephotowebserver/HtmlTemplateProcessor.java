package com.sanderbos.simplephotowebserver;

import android.app.Activity;


/**
 * String based simple HTML templating, constructing a string containing a HTML document.
 * Uses string resources for templating (stored in values.html_fragments.xml).
 * Performs string replacement based on ||keyword||.
 */
public class HtmlTemplateProcessor {

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
