package gmu.shakesafe;

/**
 * Created by Mauro on 3/11/2018.
 *
 *      This class creates a standard deviation object. The object is will keep track
 *      of all the values needed to compute the standard deviation of the magnitudes.
 *      Objects from this class are used in CalculateSD.java
 */

public class StandardDeviationObject {
    private static double[] magnitudes;
    private static int magnitudesIndex;
    private double sum, threshold, currentSD, sdMultiplier;
    private boolean arrayFull = false;

    // Constructor
    public StandardDeviationObject() {}

    public StandardDeviationObject(int sampleNum, double sdMultiplier){
        magnitudes = new double[sampleNum];
        this.sdMultiplier = sdMultiplier;
    }


    // Getters and setters

    public int getMagnitudesIndex() {return magnitudesIndex;}

    public void setMagnitudesIndex(int magnitudesIndex) {StandardDeviationObject.magnitudesIndex = magnitudesIndex;}

    public double[] getMagnitudes() {return magnitudes;}

    public void setMagnitudes(double[] magnitudes) {StandardDeviationObject.magnitudes = magnitudes;}

    public double getSum() {return sum;}

    public void setSum(double sum) {this.sum = sum;}

    public double getThreshold() {return threshold;}

    public void setThreshold(double mean) {this.threshold = mean + sdMultiplier*currentSD;}

    public double getCurrentSD() {return currentSD;}

    public void setCurrentSD(double currentSD) {this.currentSD = currentSD;}

    public boolean isArrayFull() {return arrayFull;}

    public void setArrayFull(boolean arrayFull) {this.arrayFull = arrayFull;}
}