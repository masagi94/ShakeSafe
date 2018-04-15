package gmu.shakesafe;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
//for sensor


//for push notif
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
//end push notif

//for adview
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
//end adview


//user data storage
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.*;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.UUID;
//user data storage

public class MainActivity extends AppCompatActivity implements SensorEventListener, SensorFragment.sensorValues {


    //*******************************************************************************
    // These variables are used for testing. Turn them on or off to control the app.

    public static boolean UPLOADS_ON = true;
    public static boolean NOTIFICATIONS_ON = false;

    //*******************************************************************************


    // A UUID that will be used to upload data to the cloud. This unique ID will only be generated one time.
    // After it is generated, it will be stored on the phone, and every time the phone runs it will look for the
    // folder where the file is and read the UUID for future uploads. This will help us determine which phones are
    // uploading from where.
    public static String uniqueID = "";
    public static Boolean userFileExists = false;

    //for ads
    private AdView mAdView;

    // For handling new threads
    private static Handler nHandler = new Handler();

    //for upload management
    public static boolean canUpload = true;
    public static boolean screenDelayStarted, screenDelayDone = false;

    //for creating the text file that will contain the sensor data
    private static boolean fileCreated = false;


    //following two lines are for AWS push services
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static PinpointManager pinpointManager;

    //for sensor data
    private static final int REQUEST_PERMISSION_SETTING = 101;

    private static final String TAG = "MainActivity";


    private SensorManager sensorManager;
    private Sensor accelerometer;


    /**
     * This object will track the changes in standard deviation. To change the size of the sample window,
     * change sampleNum in the parameters. The multiplier is used to adjust the threshold.
     * A multiplier of 3 would equate to: threshold = mean - 3*standardDeviation.
     * This standard deviation object is used by CalculateSD.java, as well as here.
     */
    public static StandardDeviationObject sdObject = new StandardDeviationObject(500, 3);

    public static SensorObject accSensor = new SensorObject();



    private SectionsPageAdapter mSectionsPageAdapter;
    public static ViewPager mViewPager;

    public static Context GlobalContext;


    // This array will hold all the map markers that are created when we scrape USGS.
    public static MapMarkerObject[] mapMarkers = {null, null, null, null, null, null, null, null, null, null};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //mContext = getApplicationContext();

        getPermissions();
        createUUID();
        //initializeAdViews();
        initializeSensorManager();
        initializePinpoint();

        initializeTabs();


        new ScrapeUSGS().execute();


    }



    // This method requests the required permissions from the user if the app doesn't have them already.
    private void getPermissions() {

        int Permission_All = 1;

        String[] Permissions = {Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, Permission_All);
        }
    }

    // Checks if the app has the permissions required for app execution.
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    // This creates a unique ID for the phone. Each phone will have their own unique ID.
    // These ID's will be used for uploading data to the cloud.
    private void createUUID() {
        FileOutputStream outputStream;
        File file = getFileStreamPath("UUID.txt");


        // If the UUID has not been created, make one and store it on the phone.
        if (file == null || !file.exists()) {
            uniqueID = UUID.randomUUID().toString();
            try {
                outputStream = openFileOutput("UUID.txt", Context.MODE_PRIVATE);
                outputStream.write(uniqueID.getBytes());

                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            uniqueID = getUUID();
    }

    // This returns the UUID that is already stored on the phone.
    private String getUUID() {
        String returnString = "";
        try {
            FileInputStream inputStream = openFileInput("UUID.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                returnString = stringBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(LOG_TAG, "UUID FILE READ: " + returnString);

        return returnString;
    }

    // Initializes the tab layout for the app. Can change which tab the app opens up to on startup in here.
    private void initializeTabs() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPageAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));


        // This is how we can change what tab the app opens up to. Adjust this to account for a push notifications
        // mViewPager.setCurrentItem(2);

    }

    private void proceedAfterPermission() {
        //We've got the permission, now we can proceed further
        Toast.makeText(getBaseContext(), "Using location services", Toast.LENGTH_LONG).show();
    }


    // This creates the ads that will display at the bottom of the screen.
//    private void initializeAdViews(){
//        MobileAds.initialize(this, "ca-app-pub-9747190047297291~1573255564");
//        mAdView = findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
//    }

    // Initializes the sensor manager
    private void initializeSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    // If pinpoint manager is null, this method initializes it. Called from createBundle.
    private void initializePinpoint() {

        //for push notif && user data storage
        AWSMobileClient.getInstance().initialize(this).execute();

        if (pinpointManager == null) {

            PinpointConfiguration pinpointConfig = new PinpointConfiguration(
                    getApplicationContext(),
                    AWSMobileClient.getInstance().getCredentialsProvider(),
                    AWSMobileClient.getInstance().getConfiguration());

            pinpointManager = new PinpointManager(pinpointConfig);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String deviceToken =
                                InstanceID.getInstance(MainActivity.this).getToken(
                                        "904311997649",
                                        GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                        Log.e("NotError", deviceToken);
                        pinpointManager.getNotificationClient()
                                .registerGCMDeviceToken(deviceToken);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            ).start();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if ((ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                //Got Permission
                proceedAfterPermission();
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if ((ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            //Got Permission
            proceedAfterPermission();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "-- ON PAUSE --");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "-- ON STOP --");
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

    }

    //onResume() register the accelerometer for listening the events
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "-- ON RESUME --");

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

//
//        //register notification receiver
//        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
//                new IntentFilter(PushListenerService.ACTION_PUSH_NOTIFICATION));
    }

//    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            try {
//                Log.d(LOG_TAG, "Received notification from local broadcast. Display it in a dialog.");
//
//                Bundle data = intent.getBundleExtra(PushListenerService.INTENT_SNS_NOTIFICATION_DATA);
//
//                String message = PushListenerService.getMessage(data);
//
//                new AlertDialog.Builder(MainActivity.this)
//                        .setTitle("Push notification")
//                        .setMessage(message)
//                        .setPositiveButton(android.R.string.ok, null)
//                        .show();
//            } catch (Exception e) {
//                 e.printStackTrace();
//            }
//        }
//
//    };

    // This retrieves the location and stores it into a string.
    public static String getLocation() {

        if (ActivityCompat.checkSelfPermission(GlobalContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(GlobalContext, "First enable LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            return null;
        }


        LocationManager lm = (LocationManager) GlobalContext.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            float accuracy = location.getAccuracy();
            long time = location.getTime();

            return String.valueOf(longitude) + "/" +
                    String.valueOf(latitude) + "/" +
                    String.valueOf(accuracy) + "/" +
                    String.valueOf(time);
        } catch (Exception e) {
            Toast.makeText(GlobalContext, "Location not found.", Toast.LENGTH_LONG).show();
            return null;
        }
    }


    // This updates the values on the sensor tab.
    @Override
    public void setSensorValues() {
        TextView xText = findViewById(R.id.currentX);
        TextView yText = findViewById(R.id.currentY);
        TextView zText = findViewById(R.id.currentZ);
        TextView tiltText = findViewById(R.id.current_tilt);
        TextView SDText = findViewById(R.id.StandardDeviation);

        DecimalFormat df = new DecimalFormat("##.##");

        xText.setText(df.format(accSensor.getDeltaX()));
        yText.setText(df.format(accSensor.getDeltaY()));
        zText.setText(df.format(accSensor.getDeltaZ()));
        tiltText.setText(df.format(accSensor.getTilt()));
        SDText.setText(Double.toString(sdObject.getCurrentSD()));
    }

    //need to include this because we use SensorListener
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        GlobalContext = getApplicationContext();

        new CalculateSensorData().execute(event);
        // If the current tab is the sensor tab, then update the sensors

        if (mViewPager.getCurrentItem() == 2)
            setSensorValues();
    }





    // This creates a delay after an upload occurs to stop subsequent uploads.
    public static void uploadTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.SystemClock.sleep(5000);
                nHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        canUpload = true;
                        Log.d(LOG_TAG, "READY TO UPLOAD");
                    }
                });
            }
        }).start();
    }

    // This timer will create a 10 second delay once the screen is turned off before
    // the phone can upload values
    public static void screenOffTimer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.SystemClock.sleep(10000);
                nHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        screenDelayDone = true;
                        Log.d(LOG_TAG, "SCREEN OFF TIMER: DONE");
                        Log.d(LOG_TAG, "READY TO UPLOAD");
                    }
                });
            }
        }).start();
    }



    // Function for storing data internally on the phone.
    public static void storeData() {

//        String location = getLocation();

        String[] data = getLocation().split("/");

        String userFilesData = data[0] + "/" + data[1] + "/" + data[2] + "/" + data[3];
        String activeUsersData = data[0] + "/" + data[1];


        // creates the text file to write to if it doesn't exist yet
        if (!fileCreated) {
            createTextFile();
            fileCreated = true;
        }

        FileOutputStream outputStream;

        // can use Context.MODE_APPEND to add to the end of the file...
        try {
            outputStream = GlobalContext.openFileOutput("SensorData", Context.MODE_PRIVATE);
            outputStream.write(userFilesData.getBytes());

            outputStream = GlobalContext.openFileOutput("ActiveSignal", Context.MODE_PRIVATE);
            outputStream.write(activeUsersData.getBytes());

            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createTextFile() {
        FileOutputStream outputStream, outputStream1;

        try {
            outputStream = GlobalContext.openFileOutput("SensorData", Context.MODE_PRIVATE);
            outputStream1 = GlobalContext.openFileOutput("ActiveSignal",Context.MODE_PRIVATE);
            outputStream.close();
            outputStream1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}