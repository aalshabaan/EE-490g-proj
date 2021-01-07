package ch.epfl.esl.dronereporter;

import android.content.Intent;
import android.content.pm.ActivityInfo;
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


    public static final String DRONE_OBJECT = "drone";
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
    protected void onPause() {
        super.onPause();
        if (mDrone != null && mDroneListener != null)
            mDrone.removeListener(mDroneListener);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (mDrone != null){
            if(mDroneListener == null)
                mDroneListener = new MainActivityDroneListener();
            mDrone.addListener(mDroneListener);
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
        if(mServiceDrone == null){
            Toast.makeText(this, "No drone found, connect first!", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, BebopActivity.class);
        i.putExtra(EXTRA_DEVICE_SERVICE, mServiceDrone);
        startActivity(i);
    }

    public void reviewFootage(View view){
        Intent i = new Intent(this, MainVideo.class);
        startActivity(i);
    }

    public void selectDrone(View view){
        if(!mConnected) {
            Intent i = new Intent(this, DeviceListActivity.class);
            startActivityForResult(i, SELECT_DRONE);
        }
        else{
            //if(mDrone != null){
            if(mServiceDrone != null){
                //if(mDrone.disconnect()){
                    mConnected = false;
                    mDrone.removeListener(mDroneListener);
                    mDrone.disconnect();
                    mConnectDisconnectButton.setText(getText(R.string.select_drone));
                    mServiceDrone = null;
                //}
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_DRONE && resultCode == RESULT_OK){
            /*
            mDrone = (BebopDrone) data.getSerializableExtra(DRONE_OBJECT);
            if (mDroneListener == null){
                mDroneListener = new MainActivityDroneListener();
            }
            mDrone.addListener(mDroneListener);

             */
            mServiceDrone = data.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
            mDrone = new BebopDrone(this, mServiceDrone);
            mDrone.addListener(mDroneListener);
            mConnectDisconnectButton.setText(getText(R.string.disconnect));
            mConnected = true;
            //Toast.makeText(MainActivity.this, "Activity result", Toast.LENGTH_SHORT).show();

        }

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