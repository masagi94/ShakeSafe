package gmu.shakesafe;


import android.support.v4.app.Fragment;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



/**
 * Created by Mauro on 3/3/2018.
 */

public class MapFragment extends Fragment {// implements OnMapReadyCallback {

    @Override
    public void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.map_layout, container, false);
    }
}
