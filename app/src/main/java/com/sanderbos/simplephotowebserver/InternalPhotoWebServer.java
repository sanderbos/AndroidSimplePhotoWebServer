package com.sanderbos.simplephotowebserver;

import android.os.Environment;

import com.sanderbos.simplephotowebserver.util.MyLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

/**
 * NanoHTTPD extension that drives the simple photo web server. The starting point for this server
 * was found on http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android.
 */
public class InternalPhotoWebServer extends NanoHTTPD {

    /**
     * Constructor.
     * @param port The listening port for the web server.
     */
    public InternalPhotoWebServer(int port) {
        super(port);
    }

    /**
     * Main method, that handles a HTTP request.
     * @param httpRequest The HTTP request information.
     * @return The response with the content to return to the browser.
     */
    @Override
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
}
