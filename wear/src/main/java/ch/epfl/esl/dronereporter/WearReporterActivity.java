package ch.epfl.esl.dronereporter;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class WearReporterActivity extends WearableActivity {

    public static final String RECORDING_STATE_CHANGED = "RECORDING_STATE_CHANGED";
    public static final String MEDIA_TYPE_CHANGED = "MEDIA_TYPE_CHANGED";
    public static final String MEDIA_TYPE = "MEDIA_TYPE";
    public static final String RECORDING_STATE = "recordingState";
    private final String TAG = this.getClass().getSimpleName();


    private boolean recording = false;
    private boolean videoMode = false;
    private Button mRecordButton;
    private ConstraintLayout mLayout;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission("android" + ""
                + ".permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") ==
                        PackageManager.PERMISSION_DENIED || checkSelfPermission("android" + "" +
                ".permission.INTERNET") == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android"
                    + ".permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"}, 0);
        }
        Log.d(TAG, "creating activtity");
        mRecordButton = findViewById(R.id.recordButton);

        fusedLocationClient = new FusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                TextView textViewLocation = findViewById(R.id.Location);
                for (Location location : locationResult.getLocations()) {
                    textViewLocation.setText(String.format("Lat: %s \nLong: %s", location.getLatitude(), location.getLongitude()));

                    Intent intent = new Intent(WearReporterActivity.this, WearService.class);
                    intent.setAction(WearService.ACTION_SEND.LOCATION.name());
                    intent.putExtra(WearService.LONGITUDE, location.getLongitude());
                    intent.putExtra(WearService.LATITUDE, location.getLatitude());
                    startService(intent);
                }
            }
        };


        mLayout = findViewById(R.id.containerRecording);
        // Enables Always-on
        setAmbientEnabled();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: Received a message!");
                recording = intent.getBooleanExtra(RECORDING_STATE, false);
                videoMode = intent.getBooleanExtra(MEDIA_TYPE, false);
                Log.d(TAG, "onReceive: RECIEVED THE VALUE " + videoMode + "From WearService");
                if(videoMode) {
                    if (recording) {
                        mRecordButton.setText(R.string.stopRecording);
                    } else {
                        mRecordButton.setText(R.string.startRecording);
                    }
                }
                else {
                    mRecordButton.setText(R.string.takePicture);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,new IntentFilter(RECORDING_STATE_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(MEDIA_TYPE_CHANGED));

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        updateDisplay();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.black, getTheme()));
        } else {
            mLayout.setBackgroundColor(getResources().getColor(android.R.color.white, getTheme()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest()
                .setInterval(5)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void sendRecordingSignal(View view){
        Intent intent = new Intent(this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.MEDIA_ACTION_SIGNAL.name());
        startService(intent);
        if(videoMode){
            if(recording){
                recording = false;
                mRecordButton.setText(R.string.startRecording);
            }
            else {
                recording = true;
                mRecordButton.setText(R.string.stopRecording);
            }
        }
    }

}
