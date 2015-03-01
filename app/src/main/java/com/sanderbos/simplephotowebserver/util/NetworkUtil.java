package com.sanderbos.simplephotowebserver.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Class with network utility methods.
 */
public class NetworkUtil {

    /**
     * Utility method to determine whether wifi is currently enabled on device.
     * @param context Any activity.
     * @return True in case wifi is enabled, false otherwise.
     */
    public static boolean isWifiEnabled(Activity context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo!= null && wifiNetworkInfo.isConnected();
    }

    /**
     * Utility method to get the ip address of the device.
     * Only works for IP4-addresses right now (cannot test IP6 properly).
     * @param context Any activity.
     * @return The ip-adress if it can be determined, or null in case it cannot be determined (e.g. because the device has no IP, or no IPv4 IP adress,
     * or in case of an error).
     */
    public static String getLocalIpAddress(Activity context) {
        try {
            if (isWifiEnabled(context)) {
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
