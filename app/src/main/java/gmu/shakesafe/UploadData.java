package gmu.shakesafe;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

/**
 * Created by Mauro on 3/11/2018.sds
 */

// This uploads files to the cloud. The 'key' is the folder on the cloud that the files will be stored in. If the folder
// doesn't already exist, it creates it. If it exists, it stores it in there. getFilesDir() retrieves the files stored locally
// on the phone. The local file was named 'SensorData' in another function.
public class UploadData extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... c) {
        final Context mContext = c[0];

        //final MainActivity m = new MainActivity();

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(mContext)
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();


        final TransferObserver uploadObserver =
                transferUtility.upload(
                        "s3Folder/" + MainActivity.uniqueID + ".txt",
                        new File(mContext.getFilesDir(), "SensorData"));

        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {

                    Log.d("MainActivity", "UPLOAD COMPLETE");

                    // Starts a timer to delay when the phone can upload again
                    MainActivity.uploadTimer();

                    // Handle a completed upload.
                    deleteFile(mContext);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;

                Log.d("MainActivity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");

                if (TransferState.COMPLETED == uploadObserver.getState()) {
                    // Handle a completed upload.
                    deleteFile(mContext);
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If your upload does not trigger the onStateChanged method inside your
        // TransferListener, you can directly check the transfer state as shown here.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
            Log.d("MainActivity", "UPLOAD COMPLETE");
            deleteFile(mContext);
        }

        return null;
    }

    private void deleteFile(Context mContext){
        mContext.deleteFile("SensorData");

        File file = mContext.getFileStreamPath("SensorData");

        if (file == null || !file.exists()) {
            Log.d("MainActivity", "FILE DELETED");
        } else {
            Log.d("MainActivity", "FILE NOT DELETED");
        }
    }
}