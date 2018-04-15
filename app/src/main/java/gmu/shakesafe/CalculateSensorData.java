package gmu.shakesafe;

import android.content.Context;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;


import static gmu.shakesafe.MainActivity.LOG_TAG;
import static gmu.shakesafe.MainActivity.accSensor;
import static gmu.shakesafe.MainActivity.canUpload;
import static gmu.shakesafe.MainActivity.screenDelayDone;
import static gmu.shakesafe.MainActivity.screenDelayStarted;
import static gmu.shakesafe.MainActivity.sdObject;
import static gmu.shakesafe.MainActivity.GlobalContext;


/**
 * Created by Mauro on 3/12/2018.
 *
 *      This class calculates all the values needed to display the sensor data on the Sensor tab.
 *      It also handles the Storing and Uploading of the data.
 *
 */



public class CalculateSensorData extends AsyncTask<SensorEvent, Void, Void> {

    @Override
    public Void doInBackground(SensorEvent... ev) {
        PowerManager pm = (PowerManager) GlobalContext.getSystemService(Context.POWER_SERVICE);
        SensorEvent event = ev[0];

        boolean ScreenOn;

        try {
            ScreenOn = pm.isInteractive();
        } catch (NullPointerException e) {
            ScreenOn = false;
        }

        // Only updates the sensor values if we're on the sensors tab, or the screen is off.
        // This will avoid running pointless calculations when the app is running in the background
        // and the phone is being used, or when the user isnt on the sensors tab.

        // If the screen is off, check
        //if (!ScreenOn) {



            if (MainActivity.mViewPager.getCurrentItem() == 2 || !ScreenOn) {
                double newAcc, tilt, denom, deltaX, deltaY, deltaZ;

                double realX, realY, realZ;

                // Extract the data we have stored in the sensor object
                double gravityX = accSensor.getGravityX();
                double gravityY = accSensor.getGravityY();
                double gravityZ = accSensor.getGravityZ();


                final double alpha = 0.8;

                //these values will accommodate for gravitational acceleration

                gravityX = alpha * gravityX + (1 - alpha) * event.values[0];
                gravityY = alpha * gravityY + (1 - alpha) * event.values[1];
                gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2];

                realX = Math.abs((float) (event.values[0] - gravityX));
                realY = Math.abs((float) (event.values[1] - gravityY));
                realZ = Math.abs((float) (event.values[2] - gravityZ));
                //end gravity code

                // get the change of the x,y,z values of the accelerometer
                deltaX = (Math.abs(0 - event.values[0]));
                deltaY = (Math.abs(0 - event.values[1]));
                deltaZ = (Math.abs(0 - event.values[2]));

                // if the change is below 1, it is considered noise
                if (deltaX < 1)
                    deltaX = 0;
                if (deltaY < 1)
                    deltaY = 0;
                if (deltaZ < 1)
                    deltaZ = 0;

                // deriving tilt
                denom = (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
                tilt = (float) Math.acos(deltaZ / denom);
                tilt = (float) ((tilt * 180) / 3.14);

                newAcc = Math.sqrt(realX * realX + realY * realY + realZ * realZ);


                // Calculates the standard deviation on a background thread
                new CalculateSD().execute(newAcc);

                if (tilt < 1 && !ScreenOn) {

                    if (MainActivity.UPLOADS_ON) {

                        // This creates a delay so the threshold isn't triggered when the phone's screen
                        // is turned off manually
                        if (!screenDelayStarted) {

                            screenDelayStarted = true;
                            MainActivity.screenOffTimer();
                            Log.d(LOG_TAG, "SCREEN OFF TIMER: STARTED");

                        } else if (canUpload && (newAcc >= sdObject.getThreshold()) && screenDelayDone) {

                            canUpload = false;
                            MainActivity.storeData(0, 0, 0);

                            new UploadData().execute(GlobalContext);
                            Log.d(LOG_TAG, "UPLOADING...");
                        }
                    } else
                        Log.d(LOG_TAG, "******** UPLOADS ARE DISABLED ********");
                }


                // Local calculations are done. Store values back into the sensor object for future
                // calls to this method, as well as for printing out to the screen.
                accSensor.setDeltaX(deltaX);
                accSensor.setDeltaY(deltaY);
                accSensor.setDeltaZ(deltaZ);

                accSensor.setGravityX(gravityX);
                accSensor.setGravityY(gravityY);
                accSensor.setGravityZ(gravityZ);

                accSensor.setTilt(tilt);

            }
        //}
        return null;
    }
}