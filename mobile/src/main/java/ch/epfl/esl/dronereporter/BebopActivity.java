package ch.epfl.esl.dronereporter;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class BebopActivity extends AppCompatActivity implements
        ManualControlFragment.OnJoystickMovedListener, AutopilotFragment.OnSeekBarMovedListener{

    private static final String TAG = "BebopActivity";
    public static final String RECEIVED_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String TAKE_MEDIA_ACTION = "TAKE_MEDIA_ACTION";

    //conversion from Latitude/Longitude Degrees to meters
    private static final double LATLNG_METERS = 111139;
    private static final boolean MANUAL_MODE = true;
    private static final boolean AUTO_PILOT = false;
    private static final boolean VIDEO = true;
    private static final boolean PHOTO = false;

    private BebopDrone mBebopDrone;

    private ManualControlFragment mManualControlFragment;
    private AutopilotFragment mAutoPilotFragment;


    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private H264VideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private Button mDownloadBt;
    private Button mTakePictureBt;

    private Switch mModeSwitch;
    private Switch mMediaSwitch;
    private BroadcastReceiver mWearBroadcastReceiver;
    private BroadcastReceiver mWearMediaActionReceiver;

    private byte GpsStatus;
    private double latitudeDrone;
    private double longitudeDrone;
    private double altitudeDrone;
    private double latitudeUser;
    private double longitudeUser;
    private boolean moveBool = true;
    private boolean recording = false;
    private boolean mode = false;
    private float rayon, altitude;
    private int angle;


    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    // Define a custom drone listener using an anonymous class, doing this in the Activity object
    // initialisation ensures that it is well defined by the time it is assigned to the drone in onCreate()
    private final BebopDrone.Listener mBebopListener = new BebopDrone.Listener() {


        @Override
        public void onPositionChanged(double latitude, double longitude, double altitude) {
            /*mLatitudeText.setText(String.format("%f", latitude));
            mLongitudeText.setText(String.format("%f", longitude));
            mAltitudeText.setText(String.format("%f", altitude));*/
            latitudeDrone= latitude;
            longitudeDrone = longitude;
            altitudeDrone = altitude;
            /*
            Log.d(TAG, String.valueOf(latitude));
            Log.d(TAG, String.valueOf(longitude));
            Log.d(TAG, String.valueOf(altitude));
            Log.d(TAG, "Thread position " + Thread.currentThread().getId());
            */


        }

        @Override
        public void onGpsStatusChanged(byte fixed) {
            GpsStatus = fixed;
        }

        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));

            Log.d(TAG, " Thread battery" + Thread.currentThread().getId());
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setText("Take off");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setText("Land");
                    mTakeOffLandBt.setEnabled(true);
                    mDownloadBt.setEnabled(false);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
                    mDownloadBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken" + error.toString());
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(frame);
        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {
            mDownloadProgressDialog.dismiss();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(false);
                mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mDownloadProgressDialog.setMessage("Downloading medias");
                mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                mDownloadProgressDialog.setProgress(0);
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;
            mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                mDownloadProgressDialog.dismiss();
                mDownloadProgressDialog = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop_joystick);

       mWearBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double prevLat = latitudeUser;
                double prevLong = longitudeUser;
                latitudeUser = intent.getDoubleExtra(LATITUDE,-1);
                longitudeUser = intent.getDoubleExtra(LONGITUDE,-1);
                Log.d(TAG, "watch position" + latitudeUser);
                //mLatitudeUser.setText(String.format("%f", latitudeUser));
                //mLongitudeUser.setText(String.format("%f", longitudeUser));
                double delta = Math.pow(latitudeUser-prevLat,2) + Math.pow(longitudeUser - prevLong,2);
                double threshold = 0.5/LATLNG_METERS; // in degree

                if (GpsStatus == 1 && latitudeUser != -1 && longitudeUser != -1 && moveBool &&
                        delta > Math.pow(threshold,2)){
                    moveTo();
                }

            }
        };
       mWearMediaActionReceiver = new BroadcastReceiver() {
           @Override
           public void onReceive(Context context, Intent intent) {
               if(mode == PHOTO) {
                   if (mBebopDrone.takePicture() != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)
                       Toast.makeText(BebopActivity.this, "Error taking picture, please check the drone's memory", Toast.LENGTH_SHORT).show();
               }
               else {
                   if (!recording) {
                       if (mBebopDrone.recordVideo() == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                           recording = true;
                           mTakePictureBt.setText(R.string.stopVid);
                           mMediaSwitch.setEnabled(false);
                       } else {
                           Toast.makeText(BebopActivity.this, "Error recording video, please check the drone's memory", Toast.LENGTH_SHORT).show();
                       }
                   } else {
                       if (mBebopDrone.stopRecordingVideo() == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                           recording = false;
                           mTakePictureBt.setText(R.string.vid);
                           mMediaSwitch.setEnabled(true);
                       } else {
                           Toast.makeText(BebopActivity.this, "Error stopping the recording! No idea what to do!", Toast.LENGTH_SHORT).show();
                       }
                   }
               }
           }
       };
       LocalBroadcastManager.getInstance(this).registerReceiver(mWearMediaActionReceiver, new IntentFilter(TAKE_MEDIA_ACTION));

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        //mBebopDrone = (BebopDrone) intent.getSerializableExtra(MainActivity.DRONE_OBJECT);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);
        mModeSwitch = findViewById(R.id.mode_selection_switch);
        mModeSwitch.setChecked(MANUAL_MODE);
        mMediaSwitch = findViewById(R.id.media_mode_switch);
        mMediaSwitch.setChecked(PHOTO);
        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);

        mManualControlFragment = ManualControlFragment.getInstance();
        mAutoPilotFragment = AutopilotFragment.getInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.droneControlFragmentContainer,mAutoPilotFragment).commit();
        initIHM();
        // manually call the callback to correctly engage the default mode
        switchMode(mModeSwitch);
        changeMediaMode(mMediaSwitch);
        startPositionOnWear();

    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null)
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        mBebopDrone.dispose();
        super.onDestroy();
    }

    // Starts the ReporterActivity of the watch
    public void startPositionOnWear() {
        Log.d(TAG, "connecting to the watch");
        Intent intentStartRec = new Intent(this, WearService.class);
        intentStartRec.setAction(WearService.ACTION_SEND.STARTACTIVITY.name());
        intentStartRec.putExtra(WearService.ACTIVITY_TO_START, BuildConfig.W_wearreporteractivity);
        startService(intentStartRec);
    }

    public void emergencyButtonClicked (View view){
        mBebopDrone.emergency();
    }


    private void initIHM() {
        mVideoView = (H264VideoView) findViewById(R.id.videoView);


        mTakeOffLandBt = (Button) findViewById(R.id.takeOffOrLandBt);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mBebopDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        break;
                    default:
                }
            }
        });
        mTakePictureBt = findViewById(R.id.takePictureBt);
        mTakePictureBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
                if(mMediaSwitch.isChecked() == PHOTO) {
                    if (mBebopDrone.takePicture() != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)
                        Toast.makeText(BebopActivity.this, "Error taking picture, please check the drone's memory", Toast.LENGTH_SHORT).show();
                }
                else{
                    if(!recording) {
                        if (mBebopDrone.recordVideo() == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                            recording = true;
                            mMediaSwitch.setEnabled(false);
                            Intent i = new Intent(BebopActivity.this, WearService.class);
                            i.setAction(WearService.ACTION_SEND.RECORDING_SIGNAL.name());
                            i.putExtra(BuildConfig.W_recording_path, true);
                            startService(i);
                            mTakePictureBt.setText(R.string.stopVid);
                        }
                        else {
                            Toast.makeText(BebopActivity.this, "Error recording video, please check the drone's memory", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        if (mBebopDrone.stopRecordingVideo() == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK){
                            recording = false;
                            mMediaSwitch.setEnabled(true);
                            Intent i = new Intent(BebopActivity.this, WearService.class);
                            i.setAction(WearService.ACTION_SEND.RECORDING_SIGNAL.name());
                            i.putExtra(BuildConfig.W_recording_path, false);
                            startService(i);
                            mTakePictureBt.setText(R.string.vid);
                        }
                        else{
                            Toast.makeText(BebopActivity.this, "Error stopping the recording! No idea what to do!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.getLastFlightMedias();

                mDownloadProgressDialog = new ProgressDialog(BebopActivity.this, R.style.AppCompatAlertDialogStyle);
                mDownloadProgressDialog.setIndeterminate(true);
                mDownloadProgressDialog.setMessage("Fetching medias");
                mDownloadProgressDialog.setCancelable(false);
                mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: Cancelled flight media");
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();

            }
        });
    }

    public void moveTo(){
        if (mBebopDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING
            || mBebopDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING){
            float heading; //orientation of the drone compared the the north in degrees
            double new_lat = latitudeUser - Math.sin(toRadians(angle)) * rayon / LATLNG_METERS;
            double new_long = longitudeUser - Math.cos(toRadians(angle)) * rayon / LATLNG_METERS;
            heading = (float) Math.toDegrees(Math.atan2(longitudeUser - new_long, latitudeUser - new_lat));
            ARCONTROLLER_ERROR_ENUM result = mBebopDrone.goToGPSLocation(new_lat, new_long, altitude, heading);
            if (result == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_ERROR)
                Toast.makeText(this, "move " + result, Toast.LENGTH_SHORT).show();
        }
    }



    public void switchMode(View view){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(((Switch)view).isChecked() == AUTO_PILOT){
            mModeSwitch.setThumbResource(R.drawable.ic_auto_24);
            LocalBroadcastManager.getInstance(this).registerReceiver(mWearBroadcastReceiver, new IntentFilter(RECEIVED_LOCATION));
            ft.replace(R.id.droneControlFragmentContainer, mAutoPilotFragment).commit();
            Log.d(TAG, "switchMode: AUTO");
            moveTo();
        }
        else{
            mModeSwitch.setThumbResource(R.drawable.ic_manual_24);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mWearBroadcastReceiver);
            Log.d(TAG, "switchMode: MANUAL");
            ft.replace(R.id.droneControlFragmentContainer,mManualControlFragment).commit();

        }
    }


    @Override
    public void onJoystickMoved(int joystickID, int magnitude, int angle) {
        byte x = (byte) round(magnitude * cos(toRadians(angle)));
        byte y = (byte) round(magnitude * sin(toRadians(angle)));

        switch (joystickID) {
            case ManualControlFragment.ROLL:

                if (x != 0 || y != 0) {
                    mBebopDrone.setRoll(x);
                    mBebopDrone.setPitch(y);
                    mBebopDrone.setFlag((byte) 1);
                } else {
                    mBebopDrone.setPitch((byte) 0);
                    mBebopDrone.setRoll((byte) 0);
                    mBebopDrone.setFlag((byte) 0);
                }
                break;
            case ManualControlFragment.YAW:

                if (x != 0 || y != 0) {
                    mBebopDrone.setYaw(x);
                    mBebopDrone.setGaz(y);
                } else {
                    mBebopDrone.setYaw((byte) 0);
                    mBebopDrone.setGaz((byte) 0);
                }
            }
        }

    @Override
    public void onSeekBarMoved(int seekBarID, int value) {
        switch (seekBarID){
            case AutopilotFragment.ANGLE:
                angle = value*10;
                mAutoPilotFragment.setAngle(angle);
                break;
            case AutopilotFragment.DISTANCE:
                rayon = value/2.0f;
                mAutoPilotFragment.setDistance(rayon);
                break;
            case AutopilotFragment.ALTITUDE:
                altitude = value/2.0f;
                mAutoPilotFragment.setAltitude(altitude);
        }
        moveTo();
    }

    public void changeMediaMode(View view){
        mode = ((Switch)view).isChecked();
        Intent intent = new Intent(this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.MEDIA_MODE_CHANGE_SIGNAL.name());
        intent.putExtra(BuildConfig.W_media_type_path, mode);
        startService(intent);
        if (mode == VIDEO){
            mMediaSwitch.setThumbResource(R.drawable.ic_baseline_videocam_24);
            mTakePictureBt.setText(R.string.vid);
        }
        else{
            mMediaSwitch.setThumbResource(R.drawable.ic_baseline_photo_camera_24);
            mTakePictureBt.setText(R.string.pic);
        }
    }
}
