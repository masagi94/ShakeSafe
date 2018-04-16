package gmu.shakesafe;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Created by Mauro on 3/11/2018.
 *
 *      This class is used for creating map marker objects. Each object
 *      consists of a title, magnitude, and longitude/latitude coordinates.
 *      The map markers will be populated in the ScrapeUSGS class.
 */


public class MapMarkerObject {
    private String title = "";
    private double magnitude, longitude, latitude = 0;
    private float azure = 210.0f;
    private float yellow = 60.0f;
    private float orange = 30.0f;
    private float red = 0.0f;

    //constructor
    public MapMarkerObject(){

    }

    public MapMarkerObject(String t, double mag, double lng, double lat) {
        title = t;
        magnitude = mag;
        longitude = lng;
        latitude = lat;
        //Azure = 210.0f. Orange = 30.0f, Red = 0.0f, Yellow = 60.0f

    }

    public String getTitle(){
        return title;
    }

    public double getMagnitude(){
        return magnitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public double getLatitude(){
        return latitude;
    }

    public float getColor(){

        if(4.5 <= magnitude && magnitude <= 5.4 ){
            return azure;
        }
        else if(5.5 <= magnitude && magnitude <= 6.4 ){
            return yellow;
        }
        else if(6.5 <= magnitude && magnitude <= 7.4 ){
            return orange;
        }
        else if(7.5 <= magnitude){
            return red;
        }


        return azure;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }


}