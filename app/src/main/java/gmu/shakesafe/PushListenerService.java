package gmu.shakesafe;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationClient;
import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationDetails;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.concurrent.ExecutionException;

import static gmu.shakesafe.MainActivity.LOG_TAG;
import static gmu.shakesafe.MainActivity.REAL_EARTHQUAKE;
import static gmu.shakesafe.MainActivity.pinpointManager;
import static gmu.shakesafe.MainActivity.EARTHQUAKE_COORDINATES;


/**
 * Created by Hamza on 1/3/2018.
 */

public class PushListenerService extends GcmListenerService{
    public static final String LOGTAG = PushListenerService.class.getSimpleName();

    // Intent action used in local broadcast
    public static final String ACTION_PUSH_NOTIFICATION = "push-notification";
    // Intent keys
    public static final String INTENT_SNS_NOTIFICATION_FROM = "from";
    public static final String INTENT_SNS_NOTIFICATION_DATA = "data";

    private Handler nHandler = new Handler();
    /**
     * Helper method to extract push message from bundle.
     *
     * @param data bundle
     * @return message string from push notification
     */
    public static String getMessage(Bundle data) {
        // If a push notification is sent as plain
        // text, then the message appears in "default".
        // Otherwise it's in the "message" for JSON format.
        return data.containsKey("default") ? data.getString("default") : data.getString(
                "message", "");
    }

    private void broadcast(final String from, final Bundle data) {
        Intent intent = new Intent(ACTION_PUSH_NOTIFICATION);
        intent.putExtra(INTENT_SNS_NOTIFICATION_FROM, from);
        intent.putExtra(INTENT_SNS_NOTIFICATION_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(final String from, final Bundle data) {


        // Check if pinpointManager is null. If it is, it
        // means the app was closed when the push notification came in, and
        // it will crash the app if we attempt to use pinpointManager.



            if (MainActivity.NOTIFICATIONS_ON) {

                if (pinpointManager != null) {
                    String[] notifData = data.toString().split(",");
                    String[] body;


                    for(int i = 0; i < notifData.length; i++ ){
                        Log.d(LOGTAG, "i = " + i + ": " + notifData[i].toString());
                    }




                    if (notifData.length >= 6) {
                        body = notifData[7].split("=");
                        Log.d(LOGTAG, "message = " + body[1]);


                        String[] coordinates = body[1].split("/");
                        if (coordinates.length >= 2) {
                            // Extracts the coordinates from the push notification.
                            EARTHQUAKE_COORDINATES[0] = coordinates[0];
                            EARTHQUAKE_COORDINATES[1] = coordinates[1];
                            REAL_EARTHQUAKE = true;
                            Log.d(LOGTAG, "Longitude = " + coordinates[0]);
                            Log.d(LOGTAG, "Latitude = " + coordinates[1]);
                        }
                    }


                    final NotificationClient notificationClient = pinpointManager.getNotificationClient();

                    NotificationClient.CampaignPushResult pushResult =
                            notificationClient.handleGCMCampaignPush(from, data, this.getClass());

                    if (!NotificationClient.CampaignPushResult.NOT_HANDLED.equals(pushResult)) {
                        // The push message was due to a Pinpoint campaign.
                        // If the app was in the background, a local notification was added
                        // in the notification center. If the app was in the foreground, an
                        // event was recorded indicating the app was in the foreground.
                        // For the demo, we will broadcast the notification to let the main
                        // activity display it in a dialog.
                        if (NotificationClient.CampaignPushResult.APP_IN_FOREGROUND.equals(pushResult)) {
                            // Create a message that will display the raw
                            //data of the campaign push in a dialog.
                            data.putString("message",
                                    String.format("Received Campaign Push:\n%s", data.toString()));
                            broadcast(from, data);
                        }
                    }


//                // THIS TURNS ON THE APPLICATION
//                Intent nextActivityIntent = new Intent(this, MainActivity.class);
//                startActivity(nextActivityIntent);


                } else {
                    // THIS TURNS ON THE APPLICATION
                    Intent nextActivityIntent = new Intent(this, MainActivity.class);
                    startActivity(nextActivityIntent);

                }
            }
            else
                Log.d(LOG_TAG, "******** NOTIFICATIONS ARE DISABLED ********");

    }
}
