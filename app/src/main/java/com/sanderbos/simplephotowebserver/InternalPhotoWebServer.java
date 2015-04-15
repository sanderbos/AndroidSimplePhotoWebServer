package com.sanderbos.simplephotowebserver;

import android.app.Activity;
import android.content.Context;
import android.provider.MediaStore;

import com.sanderbos.simplephotowebserver.cache.CacheDirectoryEntry;
import com.sanderbos.simplephotowebserver.cache.CacheFileEntry;
import com.sanderbos.simplephotowebserver.cache.CacheRegistry;
import com.sanderbos.simplephotowebserver.cache.ThumbnailDataCache;
import com.sanderbos.simplephotowebserver.util.MediaDirectoryFilter;
import com.sanderbos.simplephotowebserver.util.MediaStoreUtil;
import com.sanderbos.simplephotowebserver.util.MyLog;
import com.sanderbos.simplephotowebserver.util.ThumbnailUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

/**
 * NanoHTTPD extension that drives the simple photo web server. The starting point for this server
 * was found on http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android.
 */
public class InternalPhotoWebServer extends NanoHTTPD {

    /**
     * JPEG mime type (which some code treats differently from other mimetypes.
     */
    private static final String MIME_TYPE_JPEG = "image/jpeg";

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
        String pathParameter = httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_PATH);

        if (uri.startsWith(HtmlTemplateProcessor.ACTION_URL_DOWNLOAD_FILE)) {
            // Manipulate parameters as download links have the path in the URL without query parameter
            // Is of the format action{}path
            pathParameter = uri.substring(HtmlTemplateProcessor.ACTION_URL_DOWNLOAD_FILE.length());
            uri = HtmlTemplateProcessor.ACTION_URL_DOWNLOAD_FILE;
        }

        Response response;
        switch (uri) {
            case "/default_style.css":
                return getDefaultCssReponse();
            case "/about":
                return getAboutPage();
            case "/":
                return displayPhotoPageAsHtml(null);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_DIRECTORY_PAGE:
            case HtmlTemplateProcessor.ACTION_URL_SHOW_PHOTO_PAGE:
                return showMediaPage(httpRequest);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_THUMBNAIL:
                return displayImage(pathParameter, httpRequest, true, false);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_PHOTO:
                return displayImage(pathParameter, httpRequest, false, false);
            case HtmlTemplateProcessor.ACTION_URL_DOWNLOAD_FILE:
                return displayImage(pathParameter, httpRequest, false, true);
            case HtmlTemplateProcessor.ACTION_URL_SHOW_ICON:
                return displayIcon(pathParameter, httpRequest);
            default:
                response = get404Response(uri, httpRequest);
                break;
        }
        return response;
    }

    /**
     * Serve the thumbnail image to the web client.
     *
     * @param imagePath           The path to the image (which is not the path to the thumbnail, but to
     *                            the actual image).
     * @param httpRequest         The context HTTP-request.
     * @param showThumbnail       Whether or not to show the regular image (false) or its thumbnail (true).
     * @param useDownloadMimeType If set to true, the mime type used is such that it will trigger a download in the browser.
     * @return The http response (either the image, or an error page that is never seen).
     */
    private Response displayImage(String imagePath, IHTTPSession httpRequest, boolean showThumbnail, boolean useDownloadMimeType) {
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
                ResponseDataItem responseDataItem;
                if (showThumbnail) {
                    responseDataItem = getResponseDataForThumbnail(cachedFileEntry);
                } else {
                    MyLog.debug("Getting image {0}", cachedFileEntry.getFullPath());
                    String pathToServe = cachedFileEntry.getFullPath();
                    String mimeType = getMimeType(pathToServe);
                    InputStream streamToServe = new FileInputStream(pathToServe);
                    responseDataItem = new ResponseDataItem(streamToServe, mimeType);
                }
                // NanoHTTPD will close the stream.
                String mimeType = responseDataItem.getMimeType();
                if (useDownloadMimeType) {
                    mimeType = "application/octet-stream";
                }
                response = new Response(Response.Status.OK, mimeType, responseDataItem.getStreamToServe());
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
     * Serve the icon image to the web client.
     *
     * @param iconName    The name of the icon to show.
     * @param httpRequest The context HTTP-request.
     */
    private Response displayIcon(String iconName, IHTTPSession httpRequest) {
        Response response;
        int iconId = getIconResourceId(iconName);
        if (iconName == null) {
            response = get500Response(httpRequest);
        } else if (iconId == -1) {
            response = get404Response(iconName, httpRequest);
        } else {
            ResponseDataItem responseDataItem = getIconForDisplay(iconId);
            response = new Response(Response.Status.OK, responseDataItem.getMimeType(), responseDataItem.getStreamToServe());
        }
        return response;
    }

    /**
     * Convert an icon name string to id (this mechanism is used instead of just using the id to prevent hacking).
     *
     * @param iconName The string to convert to a resource id.
     * @return The resource id of the icon, or -1 in case it is not a known name.
     */
    private int getIconResourceId(String iconName) {
        int result = -1;
        switch (iconName) {
            case "logo":
                result = R.drawable.web_logo;
                break;
            case "previous":
                result = R.drawable.web_previous;
                break;
            case "next":
                result = R.drawable.web_next;
                break;
            case "download":
                result = R.drawable.web_download;
                break;
            case "expand":
                result = R.drawable.web_expand;
                break;
            case "collapse":
                result = R.drawable.web_collapse;
                break;
        }
        return result;
    }

    /**
     * Get the thumbnail data for a cached file entry, using various levels of caching (that are
     * also updated while getting the data).
     *
     * @param cachedFileEntry The file entry to get the thumbnail data for.
     * @return A response data item object representing the cached file entry.
     * @throws IOException In case of an exception while accessing the data (we do not expect
     *                     errors from this method, it should already have been checked whether the cachedFileEntry
     *                     can have a proper thumbnail, which may be generated in this method).
     */
    private ResponseDataItem getResponseDataForThumbnail(CacheFileEntry cachedFileEntry) throws IOException {
        ThumbnailDataCache thumbnailDataCache = this.cacheRegistry.getThumbnailDataCache();

        String imagePath = cachedFileEntry.getFullPath();

        String mimeType = MIME_TYPE_JPEG;
        byte[] dataToServe = thumbnailDataCache.getFromCache(imagePath);
        if (dataToServe == null) {
            // Not found in cache, retrieve it and then cache it.
            if (!cachedFileEntry.isCheckedForMediaStoreThumbnail()) {
                // First access to cached file for thumbnail access, initialize it now.
                checkMediaStoreForThumbnail(cachedFileEntry);
            }

            String thumbnailPath = cachedFileEntry.getThumbnailPath();
            if (thumbnailPath != null && new File(thumbnailPath).exists()) {
                MyLog.debug("Getting existing thumbnail {0}", thumbnailPath);
                mimeType = getMimeType(thumbnailPath);
                dataToServe = readFile(thumbnailPath);
            } else {
                MyLog.debug("Constructing new thumbnail for image {0}", cachedFileEntry.getFullPath());
                mimeType = MIME_TYPE_JPEG;
                dataToServe = ThumbnailUtil.createJPGThumbnail(cachedFileEntry.getFullPath(), HtmlTemplateProcessor.THUMBNAIL_WIDTH);
            }

            // Currently the data cache simply assumes JPEG, so it does not need to track
            // the mime type.
            if (MIME_TYPE_JPEG.equals(mimeType)) {
                thumbnailDataCache.addToCache(imagePath, dataToServe);
            }
        }

        return new ResponseDataItem(new ByteArrayInputStream(dataToServe), mimeType);
    }

    /**
     * Read a file into a byte array. The file is expected to fit into memory (contains
     * a very rudimentary check for this).
     *
     * @param path The (full) path of the file to load.
     * @return The contents of the file read into memory.
     * @throws IOException In case of errors while accessing the file (including if it does
     *                     not exist).
     */
    private byte[] readFile(String path) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(path), "r");
        try {
            int size = (int) randomAccessFile.length();
            byte[] result = new byte[size];
            randomAccessFile.readFully(result);
            return result;
        } finally {
            randomAccessFile.close();
        }
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
                    mimeType = MIME_TYPE_JPEG;
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
     * Get the about page as an HTTP response.
     *
     * @return The about page.
     */
    private Response getAboutPage() {
        String content = context.getResources().getText(R.string.about_page_html).toString();
        return new NanoHTTPD.Response(content);
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

        if (currentPathCachedDirectory != null) {
            htmlOutput.addHtmlContent("<div class='directory-box-selection'>");
            htmlOutput.addDirectoryContentToggle(currentDisplayState);
            htmlOutput.addHtmlContent("<b>" + currentPathCachedDirectory.getName() + "</b>");
            htmlOutput.addHtmlContent("</div>");
        }
        if (currentPathCachedDirectory == null || currentDisplayState.isForceShowDirectoryStructure()) {
            htmlOutput.addHtmlContent("<div class='directory-box-chooser'>");
            htmlOutput.addHtmlContent("<ul>");
            for (CacheDirectoryEntry topLevelDirectory : topLevelDirectories) {
                htmlOutput.createDirectoryTree(topLevelDirectory, currentPathCachedDirectory);
            }
            htmlOutput.addHtmlContent("</ul>");
            htmlOutput.addHtmlContent("</div>");
        }

        // If a directory is selected, show its contents as thumbnails.
        if (currentPathCachedDirectory != null) {
            String selectedThumbnailImagePath = htmlOutput.addDirectoryContentsAsThumbnails(currentPathCachedDirectory, currentDisplayState);
            if (selectedThumbnailImagePath != null && currentDisplayState.getCurrentImagePath() == null) {
                // For better user experience, default to the first thumbnail shown in case no image specifically selected.
                currentDisplayState.setCurrentImagePath(selectedThumbnailImagePath);
            }

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
            Set<String> directories = new HashSet<>();
            new MediaStoreUtil(getContext()).retrieveAllMediaDirectories(directories);

            List<String> orderedDirectories = filterAndOrderDirectories(directories);

            for (String directoryPath : orderedDirectories) {
                File directory = new File(directoryPath);
                if (directory.exists()) {
                    CacheDirectoryEntry cachedDirectoryEntry = getOrRetrieveCachedDirectory(directoryPath);
                    topLevelDirectories.add(cachedDirectoryEntry);
                }
            }
        }
    }

    /**
     * Apply the following filtering to the media directory list:
     * <ul>
     * <li>Order by name.</li>
     * <li>Ensure directories that are a subdirectory of entries also in the list are omitted.</li>
     * <li>Take out hidden directories.</li>
     * </ul>
     *
     * @param directories A set of directories to filter and sort.
     * @return A filtered sorted list of directories.
     */
    private List<String> filterAndOrderDirectories(Set<String> directories) {
        List<String> result = new ArrayList<>();

        FileFilter directoryFilter = new MediaDirectoryFilter();

        // Set up list, filtering out all directories that are a sub-directory of the list.
        for (String directory : directories) {
            int occurenceCount = 0;
            for (String checkDirectory : directories) {
                if (directory.startsWith(checkDirectory)) {
                    occurenceCount++;
                }
            }
            // Only add if there is no base directory of this directory also in the list.
            // (also take out hidden paths in the process)
            if (occurenceCount == 1 && directoryFilter.accept(new File(directory))) {
                result.add(directory);
            }
        }

        Collections.sort(result, new DirectoryPathComparator());

        return result;
    }

    /**
     * Comparator that compares directory paths based on their directory-name,
     * case insensitive and having a preference for the term 'Camera'.
     */
    private class DirectoryPathComparator implements java.util.Comparator<String> {
        /**
         * 'Camera' is a special directory name, as it is probably the main photo directory.
         */
        private static final String CAMERA_DIRECTORY_NAME = "Camera";

        /**
         * Implementation of compare method.
         *
         * @param first  The first directory path to compare.
         * @param second The second directory path to compare.
         * @return The comparison result.
         */
        @Override
        public int compare(String first, String second) {
            String firstDirectoryName = new File(first).getName();
            String secondDirectoryName = new File(second).getName();
            if (CAMERA_DIRECTORY_NAME.equals(firstDirectoryName)) {
                return -1;
            } else if (CAMERA_DIRECTORY_NAME.equals(secondDirectoryName)) {
                return 1;
            } else {
                return firstDirectoryName.compareTo(secondDirectoryName);
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
        String forceShowDirectoryStructure = httpRequest.getParms().get(HtmlTemplateProcessor.PARAMETER_FORCE_SHOW_DIRECTORY);
        result.setForceShowDirectoryStructure(forceShowDirectoryStructure != null);

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

    /**
     * Get one of the icons used in the application as a response item.
     *
     * @param iconResourceId The resource id of the resource to show.
     * @return A response data item.
     */
    private ResponseDataItem getIconForDisplay(int iconResourceId) {
        // All icons are pngs.
        String mimeType = "image/png";
        InputStream stream = getContext().getResources().openRawResource(iconResourceId);
        return new ResponseDataItem(stream, mimeType);
    }

    /**
     * Simple structure class to represent an item about to be served.
     */
    private static final class ResponseDataItem {
        /**
         * The mimetype of the response item
         */
        private String mimeType;

        /**
         * The inputstream representing the data to serve.
         */
        private InputStream streamToServe;

        /**
         * Constructor.
         *
         * @param streamToServe data to serve.
         * @param mimeType      The mime type of the data to serve.
         */
        private ResponseDataItem(InputStream streamToServe, String mimeType) {
            this.mimeType = mimeType;
            this.streamToServe = streamToServe;
        }

        /**
         * Get the mime type.
         *
         * @return mime type.
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * Get the input stream for the data.
         *
         * @return The input stream.
         */
        public InputStream getStreamToServe() {
            return streamToServe;
        }
    }


}
