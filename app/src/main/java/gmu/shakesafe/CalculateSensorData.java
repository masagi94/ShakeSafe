package gmu.shakesafe;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;


import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static gmu.shakesafe.MainActivity.LOG_TAG;
import static gmu.shakesafe.MainActivity.accSensor;
import static gmu.shakesafe.MainActivity.canUpload;
import static gmu.shakesafe.MainActivity.checkThreshold;
import static gmu.shakesafe.MainActivity.isThresholdSet;
import static gmu.shakesafe.MainActivity.isThresholdSurpassedOnce;
import static gmu.shakesafe.MainActivity.isThresholdSurpassedTwice;
import static gmu.shakesafe.MainActivity.screenDelayDone;
import static gmu.shakesafe.MainActivity.screenDelayStarted;
import static gmu.shakesafe.MainActivity.sdObject;
import static gmu.shakesafe.MainActivity.GlobalContext;
import static gmu.shakesafe.MainActivity.tremorCheckArray;
import static gmu.shakesafe.MainActivity.tremorCheckIndex;
import static gmu.shakesafe.MainActivity.userFileExists;


/**
 * Created by Mauro on 3/12/2018.
 *
 *      This class calculates all the values needed to display the sensor data on the Sensor tab.
 *      It also handles the uploading of the data.
 *
 */



public class CalculateSensorData extends AsyncTask<SensorEvent, Void, Void> {

    @Override
    public Void doInBackground(SensorEvent... ev) {
        PowerManager pm = (PowerManager) GlobalContext.getSystemService(Context.POWER_SERVICE);
        SensorEvent event = ev[0];

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = GlobalContext.registerReceiver(null, ifilter);

        // Is the phone plugged in?
        int status;
        boolean isCharging;


        AmazonS3 s3Client = new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider());


        boolean ScreenOn;

        try {
            ScreenOn = pm.isInteractive();
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (NullPointerException e) {
            ScreenOn = false;
            isCharging = false;
        }


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
        //new CalculateSD().execute(newAcc);
        if(isThresholdSurpassedOnce) {
            if(tremorCheckIndex < tremorCheckArray.length){
                tremorCheckArray[tremorCheckIndex] = newAcc;
                tremorCheckIndex++;
            }
            else{
                double sum = 0, average;

                for(int i = 0; i < tremorCheckArray.length; i++){
                    sum += tremorCheckArray[i];
                }

                average = sum / tremorCheckArray.length;

                System.out.println("AVERAGE = " + average + " THRESHOLD = " + checkThreshold);

                // If the threshold is passed again (indicating a constant shaking), then we upload
                if(average >= checkThreshold){
                    isThresholdSurpassedTwice = true;
                }
                else{
                    Log.d(LOG_TAG, "*********************** THRESHOLD RESET ***********************");
                    isThresholdSurpassedOnce = false;
                    isThresholdSet = false;
                }
            }
        }
        else{
            calculateDeviation(newAcc);
        }
        //System.out.println("MAIN, ACCELERATION: " + newAcc + " Threshold: " + sdObject.getThreshold());


        // This portion of the code uploads data if the phone screen is off,
        // the tilt is less than 1, and the phone is plugged in.
        if (tilt < 1 && !ScreenOn && isCharging) {

            if (MainActivity.UPLOADS_ON) {

                // This creates a delay so the threshold isn't triggered when the phone's screen
                // is turned off manually
                if (!screenDelayStarted) {

                    screenDelayStarted = true;
                    MainActivity.screenOffTimer();
                    Log.d(LOG_TAG, "SCREEN OFF TIMER: STARTED");




                } else if (canUpload && (newAcc >= sdObject.getThreshold()) && screenDelayDone && !isThresholdSurpassedOnce) {

                    // If the threshold once has been surpassed, then we check the next 300 samples and take their average to see
                    // if it's surpassed again.

                    Log.d(LOG_TAG, "*********************** THRESHOLD PASSED ONCE ***********************");

                    isThresholdSurpassedOnce = true;
                    tremorCheckIndex = 0;
                    //if(!isThresholdSet){
                        //isThresholdSet = false;
                        checkThreshold = sdObject.getThreshold();
                    //}


                    //System.out.println("NEW ACCELERATION: " + newAcc + " Threshold: " + sdObject.getThreshold());


                    // If the accelerometer array has an average that passes the threshold again, THEN we upload. This will prevent
                    // uploads on small things like taps, and will instead only upload when actual shaking occurs.

                }
                else if(isThresholdSurpassedTwice) {
                    Log.d(LOG_TAG, "*********************** THRESHOLD PASSED TWICE ***********************");
                    isThresholdSurpassedOnce = false;
                    isThresholdSurpassedTwice = false;
                    isThresholdSet = false;
                    canUpload = false;

                    String[] data = MainActivity.getLocation().split("/");

                    String userFilesData = data[0] + "/" + data[1] + "/" + data[2] + "/" + data[3];
                    String activeUsersData = data[0] + "/" + data[1];


                    s3Client.putObject("shakesafe-userfiles-mobilehub-889569083",
                            "ActiveUsers/" + MainActivity.uniqueID + ".txt", activeUsersData);

                    userFileExists = true;


                    s3Client.putObject("shakesafe-userfiles-mobilehub-889569083",
                            "s3Folder/" + MainActivity.uniqueID + ".txt", userFilesData);


                    Log.d(LOG_TAG, "UPLOAD COMPLETE... TIMER STARTED");
                    MainActivity.uploadTimer();
                }

            } else
                Log.d(LOG_TAG, "******** UPLOADS ARE DISABLED ********");
        }

        else {

            // This deletes the ActiveUsers file in S3 if the phone is no longer considered active.
            try {
                if(userFileExists) {

                    userFileExists = false;

                    s3Client.deleteObject(MainActivity.S3Bucket, MainActivity.uploadFileKey);

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
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

        return null;
    }



    public void calculateDeviation(double value){
        double newValue = value;
        double sum, mean, standardDeviation = 0;
        double[] magnitudeArray = sdObject.getMagnitudes();
        int magnitudeIndex = sdObject.getMagnitudesIndex();

        sum = sdObject.getSum();
        sum += newValue;

        // This populates the sd array
        if (!sdObject.isArrayFull()) {
            magnitudeArray[magnitudeIndex] = newValue;
            if (magnitudeIndex == magnitudeArray.length - 1)
                sdObject.setArrayFull(true);
            else
                magnitudeIndex++;
        } else {
            // This overwrites the oldest value in the circular array and adjusts
            // the sum of the values
            magnitudeIndex = (magnitudeIndex + 1) % magnitudeArray.length;
            sum -= magnitudeArray[magnitudeIndex];
            magnitudeArray[magnitudeIndex] = newValue;
        }

        mean = sum / magnitudeArray.length;

        for(double mgArray : magnitudeArray){
            standardDeviation += ((mgArray - mean)*(mgArray - mean));
        }

        sdObject.setMagnitudesIndex(magnitudeIndex);
        sdObject.setMagnitudes(magnitudeArray);
        sdObject.setSum(sum);
        sdObject.setCurrentSD(Math.sqrt(standardDeviation / magnitudeArray.length));
        //sdObject.setCurrentSD((standardDeviation / magnitudeArray.length));

        //System.out.println("FROM CALCULATESD, ACCELERATION: " + newValue + " Threshold: " + sdObject.getThreshold() + " Mean: " + mean);

        sdObject.setThreshold(mean);
    }


}