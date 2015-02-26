package com.sanderbos.simplephotowebserver;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class InternalWebServer extends NanoHTTPD {

    public InternalWebServer() {
        super(9009);
    }

    @Override
//    public Response serve(String uri, Method method,
//                          Map<String, String> header,
//                          Map<String, String> parameters,
//                          Map<String, String> files) {
    public Response serve(IHTTPSession session) {
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
