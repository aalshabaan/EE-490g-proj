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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class BebopActivity extends AppCompatActivity {
    private static final String TAG = "BebopActivity";
    public static final String RECEIVED_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";


    private static final double LATLNG_METERS = 111139;
    private static final boolean MANUAL_MODE = true;
    private static final boolean AUTO_PILOT = false;
    private BebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private H264VideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private Button mDownloadBt;
    private JoystickView mRollJoystick;
    private JoystickView mYawJoystick;
    private Button mMoveTo;
    private Switch mModeSwitch;
    private BroadcastReceiver mWearBroadcastReceiver;

    private byte GpsStatus;
    private double latitudeDrone;
    private double longitudeDrone;
    private double altitudeDrone;
    private double latitudeUser;
    private double longitudeUser;
    private boolean moveBool = false;


    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop_joystick);

       mWearBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "watch position");
                //Bundle b = getIntent().getExtras();
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

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        //mBebopDrone = (BebopDrone) intent.getSerializableExtra(MainActivity.DRONE_OBJECT);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);
        mRollJoystick = findViewById(R.id.rollJoystick);
        mYawJoystick = findViewById(R.id.yawJoystick);
        mModeSwitch = findViewById(R.id.mode_selection_switch);
        mModeSwitch.setChecked(AUTO_PILOT);
        initIHM();
        // manually call the callback to correctly engage the default mode
        switchMode(mModeSwitch);
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


    private void initIHM() {
        mVideoView = (H264VideoView) findViewById(R.id.videoView);

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });


        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

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

        findViewById(R.id.takePictureBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
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
                        mBebopDrone.cancelGetLastFlightMedias();
                    }
                });
                mDownloadProgressDialog.show();
            }
        });

        mRollJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                byte x = (byte) round(strength * cos(toRadians(angle)));
                byte y = (byte) round(strength * sin(toRadians(angle)));

                if(x!=0 || y!=0){
                    mBebopDrone.setRoll(x);
                    mBebopDrone.setPitch(y);
                    mBebopDrone.setFlag((byte) 1);
                }
                else{
                    mBebopDrone.setPitch((byte) 0);
                    mBebopDrone.setRoll((byte) 0);
                    mBebopDrone.setFlag((byte) 0);
                }
            }
        });


        mYawJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                byte x = (byte) round(strength * cos(toRadians(angle)));
                byte y = (byte) round(strength * sin(toRadians(angle)));

                if(x!=0 || y!=0){
                    mBebopDrone.setYaw(x);
                    mBebopDrone.setGaz(y);
                }
                else{
                    mBebopDrone.setYaw((byte) 0);
                    mBebopDrone.setGaz((byte) 0);
                }
            }
        });

        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);
        mMoveTo = findViewById(R.id.moveToBt);
    }

    public void moveTo(View view){
        moveBool = true;
        float heading; //orientation of the drone compared the the north
        double altitude = 3; //in meters above the ground
        double rayon = 1;
        double angle = Math.atan2(longitudeUser-longitudeDrone,latitudeUser-latitudeDrone);
        double new_lat = latitudeUser - Math.sin(angle)*rayon/LATLNG_METERS;
        double new_long = longitudeUser - Math.cos(angle)*rayon/LATLNG_METERS;
        heading = (float) Math.atan2(longitudeUser-new_long,latitudeUser-new_lat);
        ARCONTROLLER_ERROR_ENUM result = mBebopDrone.goToGPSLocation(new_lat, new_long, altitude, heading);
        Toast.makeText(this, "move " + result, Toast.LENGTH_SHORT).show();
    }

    public void moveTo(){
        float heading; //orientation of the drone compared the the north
        double altitude = 3; //in meters above the ground
        double rayon = 1;
        double angle = Math.atan2(longitudeUser-longitudeDrone,latitudeUser-latitudeDrone);
        double new_lat = latitudeUser - Math.sin(angle)*rayon/LATLNG_METERS;
        double new_long = longitudeUser - Math.cos(angle)*rayon/LATLNG_METERS;
        heading = (float) Math.atan2(longitudeUser-new_long,latitudeUser-new_lat);
        ARCONTROLLER_ERROR_ENUM result = mBebopDrone.goToGPSLocation(new_lat, new_long, altitude, heading);
        Toast.makeText(this, "move " + result, Toast.LENGTH_SHORT).show();
    }

    public void cancelMoveTo(View view){
        moveBool = false;
        mBebopDrone.cancelGoTo();
    }

    public void switchMode(View view){
        if(((Switch)view).isChecked() == AUTO_PILOT){
            mModeSwitch.setThumbResource(R.drawable.ic_auto_24);
            LocalBroadcastManager.getInstance(this).registerReceiver(mWearBroadcastReceiver, new IntentFilter(RECEIVED_LOCATION));

            //TODO Switch fragments
        }
        else{
            mModeSwitch.setThumbResource(R.drawable.ic_manual_24);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mWearBroadcastReceiver);
            //TODO Switch fragments
        }
    }


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
            if (fixed == 1)
                mMoveTo.setEnabled(true);
            else
                mMoveTo.setEnabled(false);
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
            Log.i(TAG, "Picture has been taken");
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
}
