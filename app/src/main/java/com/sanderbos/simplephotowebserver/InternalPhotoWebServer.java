package com.sanderbos.simplephotowebserver;

import android.app.Activity;

import fi.iki.elonen.NanoHTTPD;

/**
 * NanoHTTPD extension that drives the simple photo web server. The starting point for this server
 * was found on http://stackoverflow.com/questions/14309256/using-nanohttpd-in-android.
 */
public class InternalPhotoWebServer extends NanoHTTPD {

    /**
     * The context activity, used to resolve resources.
     */
    private Activity context;

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
        } else {
            response = get404Response(httpRequest);
        }
        return response;
    }

    /**
     * Generate a 404 error message page.
     * @param httpRequest The current request, it must already have been determined that a 404
     *                    not found response is applicable.
     * @return A 404 response with HTML message
     */
    private Response get404Response(IHTTPSession httpRequest) {
        HtmlTemplateProcessor result = new HtmlTemplateProcessor(context);
        result.set404Title(httpRequest.getUri());
        String html404Content = result.getHtmlOutput();
        Response response = new NanoHTTPD.Response(html404Content);
        response.setStatus(Response.Status.NOT_FOUND);
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
