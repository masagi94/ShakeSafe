package gmu.shakesafe;

/**
 * Created by Mauro on 3/12/2018.
 */

public class SensorObject {
    private double gravityX, gravityY, gravityZ = 0;
    private double deltaX, deltaY, deltaZ = 0;
    private double tilt = 0;



    public SensorObject(){}

    public double getGravityX() {

        return gravityX;
    }

    public void setGravityX(double gravityX) {

        this.gravityX = gravityX;
    }

    public double getGravityY() {

        return gravityY;
    }

    public void setGravityY(double gravitY) {

        this.gravityY = gravitY;
    }

    public double getGravityZ() {

        return gravityZ;
    }

    public void setGravityZ(double gravityZ) {

        this.gravityZ = gravityZ;
    }

    public double getDeltaX() {

        return deltaX;
    }

    public void setDeltaX(double deltaX) {

        this.deltaX = deltaX;
    }

    public double getDeltaY() {

        return deltaY;
    }

    public void setDeltaY(double deltaY) {

        this.deltaY = deltaY;
    }

    public double getDeltaZ() {

        return deltaZ;
    }

    public void setDeltaZ(double deltaZ) {

        this.deltaZ = deltaZ;
    }


    public double getTilt() {

        return tilt;
    }

    public void setTilt(double tilt) {

        this.tilt = tilt;
    }
}
