package gmu.shakesafe;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static gmu.shakesafe.MainActivity.mapMarkers;

/**
 * Created by Mauro on 3/11/2018.
 *
 *      This class handles which of the fragment layouts should be displayed on the main fragment.
 *      This also populates all fo the earthquake pins for the Map tab.
 */
    public class FragmentCreator extends Fragment implements OnMapReadyCallback {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        GoogleMap mGoogleMap;
        MapView mMapView;
        View mView;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FragmentCreator newInstance(int sectionNumber) {
            FragmentCreator fragment = new FragmentCreator();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }


        // This controls which of the xml files displays based on which tab the user is in.
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView;

            // Tab 1 - Information
            if(getArguments().getInt(ARG_SECTION_NUMBER) == 1){
                rootView = inflater.inflate(R.layout.information_layout, container, false);
                return rootView;
            }

            // Tab 2 - Map
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2){
                mView = inflater.inflate(R.layout.map_layout, container, false);
                return mView;
            }

            // Tab 3 - Sensors
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 3){
                rootView = inflater.inflate(R.layout.sensor_layout, container, false);

                return rootView;
            }

            // Any other tab - null
            else
                return null;
        }


        @Override
        public void onViewCreated(View view, Bundle savedInstanceState){
            super.onViewCreated(view, savedInstanceState);

            if (getArguments().getInt(ARG_SECTION_NUMBER) == 2){
                mMapView = (MapView) mView.findViewById(R.id.mapView);

                if (mMapView != null){
                    mMapView.onCreate(null);
                    mMapView.onResume();
                    mMapView.getMapAsync(this);
                }
            }

        }


        @Override
        public void onMapReady(GoogleMap googleMap){
            MapsInitializer.initialize(getContext());
            mGoogleMap = googleMap;

            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(38.8315, -77.3119))
                    .title("George Mason University"));


            googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(38.8315, -77.3119))
                    .radius(10000)
                    .strokeColor(Color.RED)
                    .fillColor(Color.TRANSPARENT));


            // Maps will center around the US and show different markers at once
            CameraPosition US = CameraPosition.builder().target(new LatLng(40.600691, -96.668929)).zoom(3).bearing(0).tilt(45).build();
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(US));


            // This while-loop populates the map tab with markers of recent earthquakes
            int i = 0;

            //BitmapDescriptorFactory color = BitmapDescriptorFactory.;

            while((i < 10) && (mapMarkers[i] != null)){
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(mapMarkers[i].getLatitude(), mapMarkers[i].getLongitude()))
                        .title(mapMarkers[i].getTitle()).snippet("Magnitude " + mapMarkers[i].getMagnitude())
                        .icon(BitmapDescriptorFactory.defaultMarker(mapMarkers[i].getColor())));
                i++;
            }

        }
    }