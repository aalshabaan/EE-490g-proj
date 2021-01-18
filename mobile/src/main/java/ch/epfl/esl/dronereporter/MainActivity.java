package ch.epfl.esl.dronereporter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;


public class MainActivity extends AppCompatActivity {


    public static final String EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE";

    private static final int SELECT_DRONE = 42;
    private static final String TAG = "MainActivity";

    private MainActivityDroneListener mDroneListener;
    private BebopDrone mDrone = null;
    private ARDiscoveryDeviceService mServiceDrone = null;
    private TextView mBatteryTextView;
    private ImageView mBatteryImageView;
    private Button mConnectDisconnectButton;
    private boolean mConnected = false;
    private boolean mTryingToPilot = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);
        mBatteryImageView = findViewById(R.id.batteryIcon);
        mBatteryTextView = findViewById(R.id.batteryPercentage);
        mConnectDisconnectButton = findViewById(R.id.selectDroneButton);
        mDroneListener = new MainActivityDroneListener();
    }

    // Add and remove the listener following the activity's life cycle

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrone != null && mDroneListener != null)
            mDrone.removeListener(mDroneListener);
    }

    @Override
    protected void onStart(){
        super.onStart();
        if (mDrone != null){
            mDrone.addListener(mDroneListener);
            mDrone.connect();
        }
    }


    public void startPositionOnWear(View view) {
        Log.d(TAG, "connecting to the watch");
        Intent intentStartRec = new Intent(MainActivity.this, WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_wearreporteractivity);
        startService(intentStartRec);
    }

    public void engageDrone(View view){
        if(!mConnected){
            Toast.makeText(this, "No drone found, connect first!", Toast.LENGTH_LONG).show();
            return;
        }
        mTryingToPilot = true;
        mDrone.disconnect();
        // Once the drone is disconnected from the Main Activity, the Listener will safely launch the piloting activity
    }

    public void reviewFootage(View view){

            Intent i = new Intent(this, MainMedia.class);
            startActivity(i);
    }

    public void selectDrone(View view){
        if(!mConnected) {
            Intent i = new Intent(this, DeviceListActivity.class);
            startActivityForResult(i, SELECT_DRONE);

        }
        else{
            if(mDrone != null)
            {
                if(mDrone.disconnect())
                mConnected = false;
                mDrone.disconnect();
                mConnectDisconnectButton.setText(getText(R.string.select_drone));

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_DRONE && resultCode == RESULT_OK){

            mServiceDrone = data.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
            mDrone = new BebopDrone(this, mServiceDrone);
            //mDrone.connect(); This is now done in OnStart to cover the case of coming back from the piloting activity
            mDrone.addListener(mDroneListener);
            mConnectDisconnectButton.setText(getText(R.string.disconnect));
            mConnected = true;


        }

    }
    
    private boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return connected;
    }

    private class MainActivityDroneListener implements BebopDrone.Listener{

        @Override
        public void onPositionChanged(double latitude, double longitude, double altitude) {

        }

        @Override
        public void onGpsStatusChanged(byte fixed) {

        }

        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            if (state == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING){
                Log.d(TAG, "onDroneConnectionChanged: Drone Connection Successful");
            }
            if (state == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED){
                if (mTryingToPilot){
                    mTryingToPilot = false;
                    Intent i = new Intent(MainActivity.this, BebopActivity.class);
                    i.putExtra(EXTRA_DEVICE_SERVICE, mServiceDrone);
                    startActivity(i);
                }
                else {
                    Log.d(TAG, "onDroneConnectionChanged: Drone Disconnected Completely" );
                }
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryTextView.setText(batteryPercentage + "%");
            if (batteryPercentage <= 30){
                mBatteryTextView.setTextColor(getColor(R.color.battery_red));
                mBatteryImageView.setImageDrawable(getDrawable(R.drawable.ic_battery_red_24));
            }
            else{
                mBatteryTextView.setTextColor(getColor(R.color.battery_green));
                mBatteryImageView.setImageDrawable(getDrawable(R.drawable.ic_battery_green_24));
            }
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {

        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {

        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {

        }

        @Override
        public void onFrameReceived(ARFrame frame) {

        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {

        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {

        }

        @Override
        public void onDownloadComplete(String mediaName) {

        }
    };

}