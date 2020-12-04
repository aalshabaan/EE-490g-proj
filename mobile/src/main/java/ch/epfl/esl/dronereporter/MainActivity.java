package ch.epfl.esl.dronereporter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;


public class MainActivity extends AppCompatActivity {


    public static final String DRONE_OBJECT = "drone";

    private static final int SELECT_DRONE = 42;
    private static final String TAG = "MainActivity";

    private MainActivityDroneListener mDroneListener;
    private BebopDrone mDrone = null;
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

    public void engageDrone(View view){
        if(mDrone == null){
            Toast.makeText(this, "No drone found, connect first!", Toast.LENGTH_LONG).show();
            return;
        }

        switch (view.getId()){
            case R.id.presenterModeButton:
                //Log.i(TAG, "engageDrone: Presenter Mode");
                Intent intent = new Intent(this, ReporterActivity.class);
                startActivity(intent);
                break;
            case R.id.documenterModeButton:
                // different Intent name because Android Studio considers all cases as the same scope
                Intent i = new Intent(this, BebopActivity.class);
                startActivity(i);
                break;
            default:
                Toast.makeText(this, "Unknown operation mode", Toast.LENGTH_SHORT).show();
        }
    }

    public void reviewFootage(View view){
        Intent i = new Intent(this, main_video.class);
        startActivity(i);
    }

    public void selectDrone(View view){
        if(!mConnected) {
            Intent i = new Intent(this, DeviceListActivity.class);
            startActivityForResult(i, SELECT_DRONE);
        }
        else{
            if(mDrone != null){
                if(mDrone.disconnect()){
                    mConnected = false;
                    mDrone.removeListener(mDroneListener);
                    mDroneListener = null;
                    mDrone = null;
                    mConnectDisconnectButton.setText(getText(R.string.select_drone));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_DRONE && resultCode == RESULT_OK){
            mDrone = (BebopDrone) data.getSerializableExtra(DRONE_OBJECT);
            if (mDroneListener == null){
                mDroneListener = new MainActivityDroneListener();
            }
            mDrone.addListener(mDroneListener);
            mConnectDisconnectButton.setText(getText(R.string.disconnect));
            mConnected = true;
        }

    }





    private class MainActivityDroneListener implements BebopDrone.Listener{
        @Override
        public void onPositionChanged(double latitude, double longitude, double altitude) {

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