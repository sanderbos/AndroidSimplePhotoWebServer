package com.sanderbos.simplephotowebserver;

import android.app.Activity;

import com.sanderbos.simplephotowebserver.cache.CacheDirectoryEntry;
import com.sanderbos.simplephotowebserver.cache.CacheFileEntry;
import com.sanderbos.simplephotowebserver.cache.CacheRegistry;
import com.sanderbos.simplephotowebserver.util.MyLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
     * Registry of cached files and directories.
     */
    private CacheRegistry cacheRegistry;

    /**
     * Constructor.
     *
     * @param port    The listening port for the web server.
     * @param context The activity context (the activity must remain valid while the server is active.
     */
    public InternalPhotoWebServer(int port, Activity context) {
        super(port);
        this.context = context;
        cacheRegistry = new CacheRegistry();
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
            return displayDirectoryContentAsHtml(null, -1);
        } else if (HtmlTemplateProcessor.ACTION_URL_SHOW_DIRECTORY.equals(uri)) {
            return showDirectory(httpRequest, httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH), getPageNumber(httpRequest));
        } else if (HtmlTemplateProcessor.ACTION_URL_SHOW_THUMBNAIL.equals(uri)) {
            return displayThumbnail(httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH), httpRequest);
        } else {
            response = get404Response(httpRequest.getUri(), httpRequest);
        }
        return response;
    }

    /**
     * Serve the thumbnail image to the web client.
     *
     * @param imagePath   The path to the image (which is not the path to the thumbnail, but to
     *                    the actual image).
     * @param httpRequest The context HTTP-request.
     * @return The http response (either the image, or an error page that is never seen).
     */
    private Response displayThumbnail(String imagePath, IHTTPSession httpRequest) {
        Response response;
        if (imagePath == null) {
            response = get500Response(httpRequest);
        } else {
            CacheFileEntry cachedFileEntry = this.cacheRegistry.getCachedFile(imagePath);
            if (cachedFileEntry == null) {
                // Then we have to create and register it here and now.
                cachedFileEntry = new CacheFileEntry(new File(imagePath), cacheRegistry);
            }
            String pathToServe = cachedFileEntry.getThumbnailPath();
            try {
                FileInputStream fileStream = new FileInputStream(pathToServe);
                response = new Response(Response.Status.OK, getMimeType(pathToServe), fileStream);
                // NanoHTTPD will close the stream.
            } catch (FileNotFoundException e) {
                return get404Response(imagePath, httpRequest);
            }
        }
        return response;
    }

    /**
     * Get a mime-type for a path, based on extension.
     * @param path The path to get the mime type for.
     * @return The mime type, or 'application/unknown' if it is not known.
     */
    private String getMimeType(String path) {
        String mimeType = "application/unknown";
        if (path != null && path.lastIndexOf('.') != -1) {
            String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            if (extension.equals("jpg") || extension.equals("jpeg")) {
                mimeType = "image/jpeg";
            } else if (extension.equals("png")) {
                mimeType = "image/png";
            } else if (extension.equals("gif")) {
                mimeType = "image/gif";
            } else if (extension.equals("html") || extension.equals("htm") ) {
                mimeType = "text/html";
            } else if (extension.equals("css")) {
                mimeType = "text/css";
            }
        }
        return mimeType;
    }

    /**
     * Generate a response with directory contents shown.
     *
     * @param httpRequest         The context request.
     * @param path                The path to show, if null a 500 page is generated.
     * @param thumbnailPageNumber The current page number is available (set to zero or -1 if not available).
     * @return The response html page.
     */
    private Response showDirectory(IHTTPSession httpRequest, String path, int thumbnailPageNumber) {
        Response response;
        if (path == null) {
            response = get500Response(httpRequest);
        } else {
            File directory = new File(path);
            if (!directory.exists()) {
                response = get404Response(path, httpRequest);
            } else {
                return displayDirectoryContentAsHtml(path, thumbnailPageNumber);
            }
        }
        return response;
    }

    /**
     * Generate a html response page for a list of directories. If there is only one directory
     * involved its images are also listed.
     *
     * @param currentPath         The currently shown directory that is also the path to highlight in the directory tree, can be null.
     * @param thumbnailPageNumber The current page number is available (set to zero or -1 if not available).
     * @return The response html page.
     */
    private Response displayDirectoryContentAsHtml(String currentPath, int thumbnailPageNumber) {
        HtmlTemplateProcessor htmlOutput = new HtmlTemplateProcessor(context);
        List<String> directories = new ArrayList<>();
        directories.add(TEMP_IMAGES_PATH);

        CacheDirectoryEntry currentPathCachedDirectory;
        if (currentPath == null) {
            currentPathCachedDirectory = null;
        } else {
            currentPathCachedDirectory = getOrRetrieveCachedDirectory(currentPath);
        }

        for (String directoryPath : directories) {
            File directory = new File(directoryPath);
            if (directory.exists()) {
                CacheDirectoryEntry cachedDirectoryEntry = getOrRetrieveCachedDirectory(directoryPath);
                htmlOutput.createDirectoryTree(cachedDirectoryEntry, currentPathCachedDirectory);

            }
        }

        // If a directory is selected, show its contents as thumbnails.
        htmlOutput.addSeparator();
        if (currentPathCachedDirectory != null) {
            htmlOutput.addDirectoryContentsAsThumbnails(currentPathCachedDirectory, thumbnailPageNumber);
            htmlOutput.addSeparator();
        }

        return new NanoHTTPD.Response(htmlOutput.getHtmlOutput());
    }

    /**
     * Get a cached version of the directory (it may be retrieved from the cache, or generated
     * now and added to the cache).
     *
     * @param directoryPath The directory to get a cached version of.
     * @return The cached directory object.
     */
    private CacheDirectoryEntry getOrRetrieveCachedDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        CacheDirectoryEntry result = cacheRegistry.getCachedDirectory(directory.getAbsolutePath());
        if (result == null) {
            // This will self-register the new entry in the cachedDirectories list
            result = new CacheDirectoryEntry(directory, cacheRegistry);
        }
        return result;
    }

    /**
     * Generate a 404 error message page.
     *
     * @param pathToPutInTitle The path or URI to put in the title of the 404 page.
     * @param httpRequest      Context request being executed.
     * @return A 404 response with HTML message
     */
    private Response get404Response(String pathToPutInTitle, IHTTPSession httpRequest) {
        MyLog.error("404 on request {0}?{1}", httpRequest.getUri(), httpRequest.getQueryParameterString());
        HtmlTemplateProcessor result = new HtmlTemplateProcessor(context);
        result.set404Title(pathToPutInTitle);
        String html404Content = result.getHtmlOutput();
        Response response = new NanoHTTPD.Response(html404Content);
        response.setStatus(Response.Status.NOT_FOUND);
        return response;
    }

    /**
     * Generate a 500 error message page, in case the URL is incorrect.
     *
     * @param httpRequest The request that could not be processed.
     * @return A 500 response with HTML message
     */
    private Response get500Response(IHTTPSession httpRequest) {
        MyLog.error("505 on request {0}?{1}", httpRequest.getUri(), httpRequest.getQueryParameterString());
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
     *
     * @return A response with the CSS content with mime type text/css
     */
    private Response getDefaultCssReponse() {
        String cssContent = context.getResources().getText(R.string.default_css).toString();

        Response response = new NanoHTTPD.Response(cssContent);
        // Css must be returned as mime-type CSS, otherwise browsers will ignore the CSS
        response.setMimeType("text/css");
        return response;
    }

    /**
     * Extract a page number from the request parameters (if a 'page' parameter is set).
     *
     * @param httpRequest The request to extract the page number from.
     * @return The page number, or -1 if none is set or could be determined.
     */
    private int getPageNumber(IHTTPSession httpRequest) {
        String pageNumberParameter = httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PAGE_NUMBER);
        int result;
        try {
            result = Integer.valueOf(pageNumberParameter);
        } catch (Exception e) {
            // For any exception, null, not a number, set value to null
            result = -1;
        }
        return result;
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
