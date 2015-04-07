package com.sanderbos.simplephotowebserver;

import android.app.Activity;
import android.content.Context;
import android.provider.MediaStore;

import com.sanderbos.simplephotowebserver.cache.CacheDirectoryEntry;
import com.sanderbos.simplephotowebserver.cache.CacheFileEntry;
import com.sanderbos.simplephotowebserver.cache.CacheRegistry;
import com.sanderbos.simplephotowebserver.util.MediaStoreUtil;
import com.sanderbos.simplephotowebserver.util.MyLog;
import com.sanderbos.simplephotowebserver.util.ThumbnailUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
     * List of top level directories, starting point into the cacheRegistry.
     */
    private List<CacheDirectoryEntry> topLevelDirectories = null;

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

        initializeCachedDirectories();

        String uri = httpRequest.getUri();
        Response response;
        switch (uri) {
            case "/default_style.css":
                return getDefaultCssReponse();
            case "/":
                return displayPhotoPageAsHtml(null);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_DIRECTORY_PAGE:
            case HtmlTemplateProcessor.ACTION_URL_SHOW_PHOTO_PAGE:
                return showMediaPage(httpRequest);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_THUMBNAIL:
                return displayImage(httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH), httpRequest, true);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_PHOTO:
                return displayImage(httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH), httpRequest, false);
            default:
                response = get404Response(uri, httpRequest);
                break;
        }
        return response;
    }

    /**
     * Serve the thumbnail image to the web client.
     *
     * @param imagePath     The path to the image (which is not the path to the thumbnail, but to
     *                      the actual image).
     * @param httpRequest   The context HTTP-request.
     * @param showThumbnail Whether or not to show the regular image (false) or its thumbnail (true).
     * @return The http response (either the image, or an error page that is never seen).
     */
    private Response displayImage(String imagePath, IHTTPSession httpRequest, boolean showThumbnail) {
        // TODO: Needs refactoring/ restructuring
        Response response;
        if (imagePath == null) {
            response = get500Response(httpRequest);
        } else {
            CacheFileEntry cachedFileEntry = this.cacheRegistry.getCachedFile(imagePath);
            if (cachedFileEntry == null) {
                // Then we have to create and register it here and now.
                cachedFileEntry = new CacheFileEntry(new File(imagePath), cacheRegistry);
            }
            try {
                InputStream streamToServe;
                String mimeType;
                if (showThumbnail) {
                    if (!cachedFileEntry.isCheckedForMediaStoreThumbnail()) {
                        // First access to cached file for thumbnail access, initialize it now.
                        checkMediaStoreForThumbnail(cachedFileEntry);
                    }

                    String thumbnailPath = cachedFileEntry.getThumbnailPath();
                    if (thumbnailPath != null) {
                        MyLog.debug("Getting existing thumbnail {0}", cachedFileEntry.getThumbnailPath());
                        mimeType = getMimeType(thumbnailPath);
                        streamToServe = new FileInputStream(thumbnailPath);
                    } else {
                        MyLog.debug("Constructing new thumbnail for image {0}", cachedFileEntry.getFullPath());
                        mimeType = getMimeType("dummy.jpg");
                        // TODO: Wait, will this generate a thumbnail in the media database?
                        byte[] thumbnailData = ThumbnailUtil.createJPGThumbnail(cachedFileEntry.getFullPath(), HtmlTemplateProcessor.THUMBNAIL_WIDTH);
                        streamToServe = new ByteArrayInputStream(thumbnailData);
                    }
                } else {
                    MyLog.debug("Getting image {0}", cachedFileEntry.getFullPath());
                    String pathToServe = cachedFileEntry.getFullPath();
                    mimeType = getMimeType(pathToServe);
                    streamToServe = new FileInputStream(pathToServe);
                }
                response = new Response(Response.Status.OK, mimeType, streamToServe);
                // NanoHTTPD will close the stream.
            } catch (FileNotFoundException e) {
                return get404Response(imagePath, httpRequest);
            } catch (IOException e) {
                MyLog.error(e.getMessage(), e);
                return get500Response(httpRequest);
            }
        }
        return response;
    }

    /**
     * Get a mime-type for a path, based on extension.
     *
     * @param path The path to get the mime type for.
     * @return The mime type, or 'application/unknown' if it is not known.
     */
    private String getMimeType(String path) {
        String mimeType = "application/unknown";
        if (path != null && path.lastIndexOf('.') != -1) {
            String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "jpg":
                case "jpeg":
                    mimeType = "image/jpeg";
                    break;
                case "png":
                    mimeType = "image/png";
                    break;
                case "gif":
                    mimeType = "image/gif";
                    break;
                case "html":
                case "htm":
                    mimeType = "text/html";
                    break;
                case "css":
                    mimeType = "text/css";
                    break;
            }
        }
        return mimeType;
    }

    /**
     * Generate a response with directory contents shown.
     *
     * @param httpRequest The context request.
     * @return The response html page.
     */
    private Response showMediaPage(IHTTPSession httpRequest) {
        Response response;
        String path = httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH);
        if (path == null) {
            response = get500Response(httpRequest);
        } else {
            File file = new File(path);
            if (!file.exists()) {
                response = get404Response(path, httpRequest);
            } else {
                MediaRequestState currentDisplayState = extractCurrentDisplayState(httpRequest);
                return displayPhotoPageAsHtml(currentDisplayState);
            }
        }
        return response;
    }

    /**
     * Generate a html response page displaying thumbnails and photos. This includes:
     * <ul>
     * <li>The directory tree.</li>
     * <li>If applicable, a list of thumbnails.</li>
     * <li>If applicable, a photo.</li>
     * </ul>
     *
     * @param currentDisplayState Information about the directory, page within that directory, and media-file to show.
     * @return The response html page.
     */
    private Response displayPhotoPageAsHtml(MediaRequestState currentDisplayState) {
        HtmlTemplateProcessor htmlOutput = new HtmlTemplateProcessor(context);

        CacheDirectoryEntry currentPathCachedDirectory;
        if (currentDisplayState == null) {
            currentPathCachedDirectory = null;
        } else {
            currentPathCachedDirectory = getOrRetrieveCachedDirectory(currentDisplayState.getCurrentDirectoryPath());
        }

        for (CacheDirectoryEntry topLevelDirectory : topLevelDirectories) {
            htmlOutput.createDirectoryTree(topLevelDirectory, currentPathCachedDirectory);
        }

        // If a directory is selected, show its contents as thumbnails.
        htmlOutput.addSeparator();
        if (currentPathCachedDirectory != null) {
            htmlOutput.addDirectoryContentsAsThumbnails(currentPathCachedDirectory, currentDisplayState);
            htmlOutput.addSeparator();
        }

        if (currentDisplayState != null && currentDisplayState.getCurrentImagePath() != null) {
            String[] siblingImagePaths = getSiblingImages(currentPathCachedDirectory, currentDisplayState.getCurrentImagePath());
            htmlOutput.addMainImageHtml(currentDisplayState.getCurrentImagePath(), siblingImagePaths[0], siblingImagePaths[1]);
            htmlOutput.addSeparator();
        }

        return new NanoHTTPD.Response(htmlOutput.getHtmlOutput());
    }

    /**
     * Determine the paths of the previous and next image in a list of images.
     *
     * @param cachedDirectory The directory in which the image resides.
     * @param imagePath       The image path to get the previous and next image for.
     * @return Always an array of two paths, with the previous and next image path filled in, or null
     * in case one or both does not exist.
     */
    private String[] getSiblingImages(CacheDirectoryEntry cachedDirectory, String imagePath) {
        String previousImagePath = null;
        String nextImagePath = null;

        List<CacheFileEntry> fileList = cachedDirectory.getFileList();
        for (int index = 0; index < fileList.size(); index++) {
            CacheFileEntry fileEntry = fileList.get(index);
            if (fileEntry.getFullPath().equals(imagePath)) {
                if (index > 0) {
                    previousImagePath = fileList.get(index - 1).getFullPath();
                }
                if (index + 1 < fileList.size()) {
                    nextImagePath = fileList.get(index + 1).getFullPath();
                }
            }
        }

        String[] result = new String[2];
        result[0] = previousImagePath;
        result[1] = nextImagePath;
        return result;
    }


    /**
     * Set up the directory structure, needed so that other methods can expect the cache to
     * be initialized even in case of a server restart.
     */
    private void initializeCachedDirectories() {
        if (topLevelDirectories == null) {
            topLevelDirectories = new ArrayList<>();

            // Set up temporary starting point for directories
            List<String> directories = new ArrayList<>();
            directories.add(TEMP_IMAGES_PATH);

            for (String directoryPath : directories) {
                File directory = new File(directoryPath);
                if (directory.exists()) {
                    CacheDirectoryEntry cachedDirectoryEntry = getOrRetrieveCachedDirectory(directoryPath);
                    topLevelDirectories.add(cachedDirectoryEntry);
                }
            }
        }
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
        String cssContent = getContext().getResources().getText(R.string.default_css).toString();

        Response response = new NanoHTTPD.Response(cssContent);
        // Css must be returned as mime-type CSS, otherwise browsers will ignore the CSS
        response.setMimeType("text/css");
        return response;
    }

    /**
     * Extract the image information to show from an http request.
     * This method assumes that it has already been verified that this request represents a valid
     * image or image directory request, error handling should have occurred before this method
     * is called.
     *
     * @param httpRequest The http request to extract the information from.
     * @return An object representing the image directory, and if present the image path
     * and current image page.
     */
    private MediaRequestState extractCurrentDisplayState(IHTTPSession httpRequest) {
        MediaRequestState result = new MediaRequestState();
        // See if a page number is explicitly set on request, may be overridden later in case
        // a picture is specified.
        result.setCurrentThumbnailPage(getPageNumber(httpRequest));
        String uri = httpRequest.getUri();
        String path = httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH);
        boolean pathRepresentsDirectory = HtmlTemplateProcessor.ACTION_URL_SHOW_DIRECTORY_PAGE.equals(uri);
        if (pathRepresentsDirectory) {
            result.setCurrentDirectoryPath(path);
            result.setCurrentImagePath(null);
        } else {
            result.setCurrentImagePath(path);
            result.setCurrentDirectoryPath(getDirectoryFromFilePath(path));
            int extractedCurrentPage = extractCurrentThumbnailPage(path, result.getCurrentDirectoryPath());
            if (extractedCurrentPage != -1) {
                result.setCurrentThumbnailPage(extractedCurrentPage);
            }
        }
        return result;
    }

    /**
     * Get the parent folder of a file, using a naive implementation as all paths represented should be full paths.
     *
     * @param filePath The file to get the directory path from.
     * @return The full path the the directory of filePath.
     */
    private String getDirectoryFromFilePath(String filePath) {
        return new File(filePath).getParentFile().getAbsolutePath();
    }

    /**
     * Locate what thumbnail page the image being displayed
     *
     * @param imagePath     The image being shown (should not be null at this point).
     * @param directoryPath The directory in which the file resides.
     * @return The thumbnail page the image resides on, or -1 if no such page could be determined.
     */
    private int extractCurrentThumbnailPage(String imagePath, String directoryPath) {
        int result = -1;
        CacheDirectoryEntry cacheDirectoryEntry = getOrRetrieveCachedDirectory(directoryPath);
        List<CacheFileEntry> fileList = cacheDirectoryEntry.getFileList();
        for (int index = 0; index < fileList.size(); index++) {
            if (fileList.get(index).getFullPath().equals(imagePath)) {
                // Found image, so on what page are we now?
                result = index / HtmlTemplateProcessor.THUMBNAIL_PAGE_SIZE;
                break;
            }
        }
        return result;
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

    /**
     * Get a reference to the Android context.
     *
     * @return The (a) context.
     */
    private Context getContext() {
        return context;
    }

    /**
     * Try to find a path to an existing thumnbnail for an image, in the Android media store.
     * The cachedFileEntry is updated in case a thumbnail is found (and in any case the state
     * is updated that this query was performed).
     *
     * @param cachedFileEntry The file entry to determine a thumbnail for, and is updated with
     *                        thumbnail information in this method.
     */
    private void checkMediaStoreForThumbnail(CacheFileEntry cachedFileEntry) {
        // Whatever happens next, we checked for a thumbnail.
        cachedFileEntry.setCheckedForMediaStoreThumbnail(true);

        // If a thumbnail path was already set, we are done.
        if (cachedFileEntry.getThumbnailPath() == null) {
            long imageFileId = new MediaStoreUtil(context).getImageIdForPath(cachedFileEntry.getFullPath());

            if (imageFileId != -1) {
                String mediaStoreQuery = MediaStore.Images.Thumbnails.IMAGE_ID + " = ? AND "
                        + MediaStore.Images.Thumbnails.KIND + " = "
                        + MediaStore.Images.Thumbnails.MINI_KIND;

                String thumbnailPath = new MediaStoreUtil(context).performMediaStoreQueryWithSingleStringResult(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        mediaStoreQuery, String.valueOf(imageFileId), MediaStore.Images.Thumbnails.DATA);
                // Sanity check, we expect to get a JPEG image here, otherwise be safe and just ignore the Android storage.
                if (thumbnailPath != null && thumbnailPath.toLowerCase().endsWith("jpg")) {
                    MyLog.debug("Found thumbnail {0}", thumbnailPath);
                    cachedFileEntry.setThumbnailPath(thumbnailPath);
                }

            }
        }
    }

}
