package gmu.shakesafe;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Mauro on 3/3/2018.
 */

public class SensorFragment extends Fragment {


    // This interface is used in MainActivity to update the values of the sensors displayed on the first tab
    public interface sensorValues{
        void setSensorValues();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.sensor_layout, container, false);
    }


}
