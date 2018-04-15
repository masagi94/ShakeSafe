package gmu.shakesafe;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static gmu.shakesafe.MainActivity.LOG_TAG;
import static gmu.shakesafe.MainActivity.mapMarkers;
/**
 * Created by Mauro on 3/7/2018.
 *
 *      This class is used to scrape USGS of their most recent earthquake publications. The
 *      scraping is performed on another thread.
 */

public class ScrapeUSGS extends AsyncTask<Void, Void, Void>{

    //String words;


    // This helper method handles the connection to the website passed in as a parameter.
    private String request(String uri) throws Exception {

        StringBuilder sb = new StringBuilder();
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader bin = new BufferedReader(new InputStreamReader(in));

            String inputLine;

            while ((inputLine = bin.readLine()) != null) {
                sb.append(inputLine);
            }
        } finally {
            // regardless of success or failure, we will disconnect from the url connection.
            urlConnection.disconnect();
        }

        return sb.toString();
    }


    @Override
    protected Void doInBackground(Void... params){

        Log.d(LOG_TAG, "CREATING MAP MARKERS.");

        /** To change the types of earthquakes we read, change the end of the url below like this:

         Past Hour:
         Significant: significant_hour.geojson
         M 4.5+: 4.5_hour.geojson
         M 2.5+: 2.5_hour.geojson
         M 1.0+: 1.0_hour.geojson
         All : all_hour.geojson

         Past Day:
         Significant: significant_day.geojson
         M 4.5+: 4.5_day.geojson
         M 2.5+: 2.5_day.geojson
         M 1.0+: 1.0_day.geojson
         All : all_day.geojson

         Past Week:
         Significant: significant_week.geojson
         M 4.5+: 4.5_week.geojson
         M 2.5+: 2.5_week.geojson
         M 1.0+: 1.0_week.geojson
         All : all_week.geojson

         Past Month:
         Significant: significant_month.geojson
         M 4.5+: 4.5_month.geojson
         M 2.5+: 2.5_month.geojson
         M 1.0+: 1.0_month.geojson
         All : all_month.geojson
         */

        String urlStr = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson";

        try {
            // Stores the contents of the URL provided into a string, which can then be used
            // to extract the data we are looking for (title, magnitude, coordinates)
            String request = request(urlStr);
            JSONObject root = new JSONObject(request);
            JSONArray features = root.getJSONArray("features");




            // This loops through the "features" section of the geoJSON, in search of our values.
            // A new MapMarkerObject is created for every earthquake found. The MapMarkerObject
            // will contain all the data we need to display the earthquakes on the map tab.
            for (int i = 0; i < features.length() && i < 10; i++){
                MapMarkerObject marker = new MapMarkerObject();

                JSONObject feature = features.getJSONObject(i);

                // The title and magnitude of the earthquake can be found under the "properties"
                // tag of the geoJSON. The values are stored in the MapMarkerObject.
                JSONObject properties = feature.getJSONObject("properties");
                marker.setTitle(properties.getString("place"));
                marker.setMagnitude(properties.getDouble("mag"));

                // The coordinates are found under the "geometry" tag of the geoJSON. The values
                // are stored in the MapMarkerObject.
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                marker.setLongitude(coordinates.getDouble(0));
                marker.setLatitude(coordinates.getDouble(1));

                // Stores the new map marker into an array for later use
                mapMarkers[i] = marker;
            }

            Log.d("USGS GEOJSON PARSER: ", "COMPLETED");
        }
        catch(Exception e){
            //e.printStackTrace();
            Log.d("USGS GEOJSON PARSER: ", "FAILED");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

}
