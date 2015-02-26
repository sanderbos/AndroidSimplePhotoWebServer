package com.sanderbos.simplephotowebserver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class SimplePhotoWebServerActivity extends ActionBarActivity {

    InternalWebServer internalWebServer;

    private Button startButton;
    private Button stopButton;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_photo_web_server);

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        statusTextView = (TextView) findViewById(R.id.statusText);

        updateGUIStatus();

        //startWebServer();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_simple_photo_web_server, menu);
        return true;
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopWebServer();
    }

    @Override
    protected void onStop() {
        stopWebServer();
        super.onStop();
    }

    public void onStartButtonClicked(View view) {
        startWebServer();
    }

    public void onStopButtonClicked(View view) {
        stopWebServer();
    }

    private boolean startWebServer() {
        MyLog.debug("startWebServer called");
        stopWebServer();
        internalWebServer = new InternalWebServer();
        try {
            internalWebServer.start();
        } catch(IOException ioException) {
            MyLog.error("The server could not start.", ioException);
        }
        MyLog.debug("Web server initialized.");
        updateGUIStatus();
        return true;
    }

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

    private void updateGUIStatus() {
        if (!isWifiEnabled()) {
            statusTextView.setText("Please enable Wifi access first");
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
        } else if (isWebServerRunning()) {
            String publicURL = getPublicURL();
            statusTextView.setText("Server is running on " + publicURL);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusTextView.setText("Server is not running");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private String getPublicURL() {
        String result = null;
        if (isWifiEnabled() && isWebServerRunning()) {
            result = "http://" + getLocalIpAddress() + ":" + internalWebServer.getListeningPort() + "/";
        }
        return result;
    }

    private boolean isWebServerRunning() {
        return internalWebServer != null && internalWebServer.isAlive();
    }

    private boolean isWifiEnabled() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo!= null && wifiNetworkInfo.isConnected();
    }

    private String getLocalIpAddress() {
        try {
            if (isWifiEnabled()) {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String ip = inetAddress.getHostAddress();
                            MyLog.debug("***** IP=" + ip);
                            return ip;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            MyLog.error(ex.toString(), ex);
        }
        return null;
    }
}
