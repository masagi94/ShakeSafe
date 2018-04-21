package gmu.shakesafe;

import android.os.AsyncTask;

import static gmu.shakesafe.MainActivity.sdObject;

/**
 * Created by Mauro on 3/11/2018.
 *
 *      This class will calculate the standard deviation on a separate thread.
 */

public class CalculateSD extends AsyncTask<Double, Void, Void> {

    @Override
    protected Void doInBackground(Double... value) {

        double newValue = value[0];
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

        System.out.println("FROM CALCULATESD, ACCELERATION: " + newValue + " Threshold: " + sdObject.getThreshold() + " Mean: " + mean);

        sdObject.setThreshold(mean);


        return null;
    }
}