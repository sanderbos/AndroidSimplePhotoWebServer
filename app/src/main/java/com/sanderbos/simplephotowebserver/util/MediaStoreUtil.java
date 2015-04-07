package com.sanderbos.simplephotowebserver.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

/**
 * Helper class with 'fuzzy' access to the media store.
 */
public class MediaStoreUtil {

    /**
     * The context activity, used to resolve resources.
     */
    private Context context;

    /**
     * Constructor.
     *
     * @param context Context to use for queries.
     */
    public MediaStoreUtil(Context context) {
        this.context = context;
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
     * Helper routine to query the media database with a query with a single argument and a single expected result.
     *
     * @param externalContentURI The content URI of the content to query.
     * @param mediaQuery         The media query with exactly one argument (so one '?' is expected in this query).
     * @param queryArgument      The string argument to pass into the query.
     * @param targetColumnName   The column name whose value to retrieve.
     * @return The single result value, or null in case the query did not have any result.
     */
    public String performMediaStoreQueryWithSingleStringResult(Uri externalContentURI, String mediaQuery, String queryArgument, String targetColumnName) {
        String result = null;

        String[] projection = {targetColumnName};
        String[] arguments = {queryArgument};

        Cursor mediaCursor = performMediaStoreQuery(externalContentURI, mediaQuery, projection, arguments);
        try {
            if (mediaCursor.moveToFirst()) {
                // Only one column is selected in the projection, so can safely get the first column
                int columnIndex = mediaCursor.getColumnIndexOrThrow(targetColumnName);
                result = mediaCursor.getString(columnIndex);
            }
        } finally {
            mediaCursor.close();
        }
        return result;
    }

    /**
     * Get the image id for an image path.
     * A problem here is that the image path as stored in the media store is not canonical, so if there
     * are multiple paths possible for one image (which is the case at least on Samsung devices, with many paths
     * pointing to the same physical directory) you cannot use the image path in the query. So instead use a query
     * on filename and then find the best match path wise.
     *
     * @param imagePath The image path (should be a full path) to look up in the media store.
     * @return The image id found, or -1 in case the image path could not be located in the media store.
     */
    public long getImageIdForPath(String imagePath) {
        long resultId = -1;

        String fileName = new File(imagePath).getName();

        String mediaStoreQuery = MediaStore.Images.ImageColumns.DISPLAY_NAME + " = ?";
        String[] projection = {MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA};
        String[] arguments = {fileName};
        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor mediaCursor = performMediaStoreQuery(contentUri, mediaStoreQuery, projection, arguments);
        try {
            int columnIndexId = mediaCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
            int columnIndexData = mediaCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA);

            // This is somewhat fuzzy, but score 1 as starting point means we want at least one directory matching in the paths.
            int bestFoundScore = 1;
            while (mediaCursor.moveToNext()) {
                String mediaStorePath = mediaCursor.getString(columnIndexData);

                int comparisonScore = comparePathsForScore(imagePath, mediaStorePath);

                if (comparisonScore > bestFoundScore) {
                    resultId = mediaCursor.getInt(columnIndexId);
                    bestFoundScore = comparisonScore;
                }
            }
        } finally {
            mediaCursor.close();
        }

        return resultId;
    }

    private Cursor performMediaStoreQuery(Uri contentUri, String mediaStoreQuery, String[] projection, String[] arguments) {
        return getContext().getContentResolver().query(contentUri, projection,
                mediaStoreQuery, arguments, null);
    }

    /**
     * Compare two paths, by checking how much of the subdirectories match.
     * This kind of fuzzy comparison is done because:
     * <ul>
     * <li>The media store paths are not canonical, so multiple paths may point to the same file.</li>
     * <li>In any case, File.getCanonicalPath() does not appear to work in Android, on a Samsung Galaxy S3
     * directories like storage/emulated/legacy/ and storage/emulated/0/ are both seen as a canonical form.</li>
     * </ul>
     *
     * @param sourcePath     The path to compare to.
     * @param pathToEvaluate The path to compare
     * @return The comparison score (&gt;= 0), with one score for each path part that matches.
     */
    private int comparePathsForScore(String sourcePath, String pathToEvaluate) {
        int score = 0;
        File sourceFile = new File(sourcePath);
        File targetFile = new File(pathToEvaluate);
        do {
            if (sourceFile.getName().equals(targetFile.getName())) {
                score++;
            }

            sourceFile = sourceFile.getParentFile();
            targetFile = targetFile.getParentFile();

            // Quit as soon as source or target does not have a parent anymore
        } while (sourceFile != null && targetFile != null);

        return score;
    }


}
