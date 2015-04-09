package com.sanderbos.simplephotowebserver;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.sanderbos.simplephotowebserver.util.MyLog;
import com.sanderbos.simplephotowebserver.util.NetworkUtil;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Main activity for project, showing the status and allowing the server to be stopped and started.
 */
public class SimplePhotoWebServerActivity extends ActionBarActivity {

    /**
     * Reference to running web server daemon (null if not currently running).
     */
    InternalPhotoWebServer internalWebServer;

    /**
     * Reference to start button.
     */
    private Button startButton;

    /**
     * Reference to stop button.
     */
    private Button stopButton;

    /**
     * Reference to status text.
     */
    private TextView statusTextView;

    /**
     * Reference to status text for the current URL.
     */
    private TextView urlTextView;

    /**
     * onCreate implementation, get references to screen items and set up initial state.
     *
     * @param savedInstanceState Previous state of activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_photo_web_server);

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        statusTextView = (TextView) findViewById(R.id.statusText);
        urlTextView = (TextView) findViewById(R.id.urlText);

        // Not possible unfortunately, because of current minimum API-level.
        //ActionBar actionBar = getActionBar();
        //actionBar.setIcon(R.mipmap.ic_launcher);

        updateGUIStatus();

    }

    /**
     * onCreateOptionsMenu implementation.
     *
     * @param menu Reference to menu.
     * @return Return false to not show menu initially.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_simple_photo_web_server, menu);
        return false;
    }

    /**
     * onOptionsItemSelected implementation.
     *
     * @param item The menu item being clicked.
     * @return True in case menu item selection has been consumed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * onDestroy implementation, stop web server if it is currently running.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWebServer();
    }

    /**
     * onStop implementation, stop web server if it is currently running.
     */
    @Override
    protected void onStop() {
        stopWebServer();
        super.onStop();
    }

    /**
     * Handler for click of start button, start the web server.
     *
     * @param view The button clicked.
     */
    public void onStartButtonClicked(View view) {

        startWebServer();
    }

    /**
     * Handler for click of stop button, stop the web server.
     *
     * @param view The button clicked.
     */
    public void onStopButtonClicked(View view) {
        stopWebServer();
    }

    /**
     * Start the web server in the background.
     *
     * @return Whether the web server was started (currently always true).
     */
    private boolean startWebServer() {
        MyLog.debug("startWebServer called");
        stopWebServer();
        int port = Integer.valueOf(getResources().getText(R.string.number_default_httpd_port).toString());
        internalWebServer = new InternalPhotoWebServer(port, this);
        try {
            internalWebServer.start();
        } catch (IOException ioException) {
            MyLog.error("The server could not start.", ioException);
        }
        MyLog.info("Web server initialized.");
        updateGUIStatus();
        return true;
    }

    /**
     * Stop the web server, if one is running  in the background.
     *
     * @return Whether the web server was stopped (currently always true).
     */
    private boolean stopWebServer() {
        MyLog.debug("stopWebServer called");
        boolean stopped = false;
        if (isWebServerRunning()) {
            internalWebServer.stop();
            internalWebServer = null;
            stopped = true;
            MyLog.info("Web server stopped.");
        }
        updateGUIStatus();
        return stopped;
    }

    /**
     * Check the current status of the web server background thread, and the wifi status, and
     * set the status text fields and enable or disable the buttons appropriately.
     */
    private void updateGUIStatus() {
        if (!NetworkUtil.isWifiEnabled(this)) {
            statusTextView.setText(getResources().getText(R.string.msg_enable_wifi));
            urlTextView.setText("");
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (isWebServerRunning()) {
            String publicURL = getPublicURL();
            statusTextView.setText(getResources().getText(R.string.msg_server_running));
            urlTextView.setText(publicURL);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            statusTextView.setText(getResources().getText(R.string.msg_server_not_running));
            urlTextView.setText("");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Construct the URL by which the web server is accessible.
     *
     * @return The public URL, or null in case there is not currently a web server running.
     */
    private String getPublicURL() {
        String result = null;
        if (NetworkUtil.isWifiEnabled(this) && isWebServerRunning()) {
            result = MessageFormat.format("http://{0}:{1}/", NetworkUtil.getLocalIpAddress(this), String.valueOf(internalWebServer.getListeningPort()));
        }
        return result;
    }

    /**
     * Determine from the internalWebServer field whether a web server background thread is currently running.
     *
     * @return True in case a web server thread is currently running in the background, false if it is not.
     */
    private boolean isWebServerRunning() {
        return internalWebServer != null && internalWebServer.isAlive();
    }

}
