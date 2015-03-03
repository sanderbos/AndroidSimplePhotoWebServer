package com.sanderbos.simplephotowebserver;

import android.app.Activity;

import com.sanderbos.simplephotowebserver.cache.CacheDirectoryEntry;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * NanoHTTPD extension that drives the simple photo web server. The starting point for this server
 * was found on http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android.
 */
public class InternalPhotoWebServer extends NanoHTTPD {

    /**
     * Temporary hard coded images path.
     */
    private static final String TEMP_IMAGES_PATH = "/storage/sdcard0/WhatsApp/Media/WhatsApp Images";

    /**
     * The context activity, used to resolve resources.
     */
    private Activity context;

    /**
     * All cached directory entries share the same map of all found cached directories, kept in
     * this web server object.
     */
    private Map<File, CacheDirectoryEntry> cachedDirectories = new HashMap<>();

    /**
     * Constructor.
     *
     * @param port    The listening port for the web server.
     * @param context The activity context (the activity must remain valid while the server is active.
     */
    public InternalPhotoWebServer(int port, Activity context) {
        super(port);
        this.context = context;
    }

    /**
     * Main method, that handles a HTTP request.
     *
     * @param httpRequest The HTTP request information.
     * @return The response with the content to return to the browser.
     */
    @Override
    public Response serve(IHTTPSession httpRequest) {

        String uri = httpRequest.getUri();
        Response response;
        if ("/default_style.css".equals(uri)) {
            return getDefaultCssReponse();
        } else if ("/".equals(uri)) {
            return displayDirectoryContentAsHtml(null);
        } else if (HtmlTemplateProcessor.ACTION_SHOW_DIR_NAME.equals(uri)) {
            return showDirectory(httpRequest, httpRequest.getParms().get("path"));
        } else {
            response = get404Response(httpRequest.getUri());
        }
        return response;
    }

    /**
     * Generate a response with directory contents shown.
     * @param httpRequest The context request.
     * @param path The path to show, if null a 500 page is generated.
     * @return The response html page.
     */
    private Response showDirectory(IHTTPSession httpRequest, String path) {
        Response response;
        if (path == null) {
            response = get500Response(httpRequest);
        } else {
            File directory = new File(path);
            if (!directory.exists()) {
                response = get404Response(path);
            } else {
                return displayDirectoryContentAsHtml(path);
            }
        }
        return response;
    }

    /**
     * Generate a html response page for a list of directories. If there is only one directory
     * involved its images are also listed.
     * @param currentPath The currently shown directory that is also the path to highlight in the directory tree, can be null.
     * @return The response html page.
     */
    private Response displayDirectoryContentAsHtml(String currentPath) {
        HtmlTemplateProcessor htmlOutput = new HtmlTemplateProcessor(context);
        List<String> directories = new ArrayList<>();
        directories.add(TEMP_IMAGES_PATH);

        CacheDirectoryEntry currentPathCachedDirectory;
        if (currentPath == null) {
            currentPathCachedDirectory = null;
        } else {
            currentPathCachedDirectory = getOrRetrieveCachedDirectory(new File(currentPath));
        }

        for (String directoryPath: directories) {
            File directory = new File(directoryPath);
            if (directory.exists()) {
                CacheDirectoryEntry cachedDirectoryEntry = getOrRetrieveCachedDirectory(directory);
                htmlOutput.createDirectoryTree(cachedDirectoryEntry, currentPathCachedDirectory);

            }
        }

        // If a directory is selected, show its contents as thumbnails.
        htmlOutput.addSeparator();
        if (currentPathCachedDirectory != null) {
            htmlOutput.addDirectoryContentsAsThumbnails(currentPathCachedDirectory);
            htmlOutput.addSeparator();
        }

        return new NanoHTTPD.Response(htmlOutput.getHtmlOutput());
    }

    /**
     * Get a cached version of the directory (it may be retrieved from the cache, or generated
     * now and added to the cache).
     * @param directory The directory to get a cached version of.
     * @return The cached directory object.
     */
    private CacheDirectoryEntry getOrRetrieveCachedDirectory(File directory) {
        CacheDirectoryEntry result;
        if (cachedDirectories.containsKey(directory)) {
            result = cachedDirectories.get(directory);
        } else {
            // This will self-register the new entry in the cachedDirectories list
            result = new CacheDirectoryEntry(directory, cachedDirectories);
        }
        return result;
    }

    /**
     * Generate a 404 error message page.
     * @param pathToPutInTitle The path or URI to put in the title of the 404 page.
     * @return A 404 response with HTML message
     */
    private Response get404Response(String pathToPutInTitle) {
        HtmlTemplateProcessor result = new HtmlTemplateProcessor(context);
        result.set404Title(pathToPutInTitle);
        String html404Content = result.getHtmlOutput();
        Response response = new NanoHTTPD.Response(html404Content);
        response.setStatus(Response.Status.NOT_FOUND);
        return response;
    }

    /**
     * Generate a 500 error message page, in case the URL is incorrect.
     * @param httpRequest The request that could not be processed.
     * @return A 500 response with HTML message
     */
    private Response get500Response(IHTTPSession httpRequest) {
        HtmlTemplateProcessor result = new HtmlTemplateProcessor(context);
        String requestPath = MessageFormat.format("{0}?{1}", httpRequest.getUri(), httpRequest.getQueryParameterString());
        result.set500Title(requestPath);
        String html404Content = result.getHtmlOutput();
        Response response = new NanoHTTPD.Response(html404Content);
        response.setStatus(Response.Status.INTERNAL_ERROR);
        return response;
    }

    /**
     * Get the default CSS content as response.
     * @return A response with the CSS content with mime type text/css
     */
    private Response getDefaultCssReponse() {
        String cssContent = context.getResources().getText(R.string.default_css).toString();

        Response response = new NanoHTTPD.Response(cssContent);
        // Css must be returned as mime-type CSS, otherwise browsers will ignore the CSS
        response.setMimeType("text/css");
        return response;
    }



    /*
    public Response serve(IHTTPSession httpRequest) {
        String answer = "";
        try {
            // Open file from SD Card
            File root = Environment.getExternalStorageDirectory();
            String filePath = root.getAbsolutePath() +
                    "/www/index.html";
            MyLog.info("Reading " + filePath);
            FileReader index = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(index);
            String line = "";
            while ((line = reader.readLine()) != null) {
                answer += line;
            }
            reader.close();

        } catch (IOException ioException) {
            MyLog.error(ioException.toString(), ioException);
        }


        return new NanoHTTPD.Response(answer);
    }
     */
}
