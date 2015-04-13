package com.sanderbos.simplephotowebserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

/**
 * Broadcast receiver listening to changes in the wifi connection, and notify the parent activity.
 * (based on code from https://www.grokkingandroid.com/android-getting-notified-of-connectivity-changes/).
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {

    /**
     * The parent activity to notify of network changes.
     */
    SimplePhotoWebServerActivity parentActivity;

    /**
     * Constructor.
     *
     * @param parentActivity The parent activity that is not be notified of network changes.
     */
    public WifiBroadcastReceiver(SimplePhotoWebServerActivity parentActivity) {
        this.parentActivity = parentActivity;
    }

    /**
     * onReceive implementation, notify parent activity of possible network change.
     *
     * @param context The context of the broadcast.
     * @param intent  The intent being broadcast.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // When this method is called it will already be a ConnectivityManager.CONNECTIVITY_ACTION.

        // Unclear whether this is really clean way to call activity that may be suspended, but
        // doing extra broadcast redirection does not help, and this broadcast manager is now
        // deregistered as soon as the activity is paused.
        this.parentActivity.onWifiChange();
    }

    /**
     * Register this broadcast receiver (place code in this class since it is relevant what
     * intents are to be received.
     *
     * @param context A valid context.
     */
    public void registerReceiver(ContextWrapper context) {
        context.registerReceiver(this,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Unregister this broadcast receiver.
     *
     * @param context A valid context.
     */
    public void unregisterReceiver(ContextWrapper context) {
        context.unregisterReceiver(this);
    }
}
