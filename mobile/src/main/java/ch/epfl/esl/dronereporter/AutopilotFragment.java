package ch.epfl.esl.dronereporter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AutopilotFragment#getInstance()} factory method to
 * create an instance of this fragment.
 */
public class AutopilotFragment extends Fragment {

    public static final int DISTANCE = 0;
    public static final int ANGLE = 1;
    public static final int ALTITUDE = 2;

    private static final String TAG = "AutoPilotFragment";

    private SeekBar mDistanceSeekBar, mAngleSeekBar, mAltitudeSeekBar;
    private static AutopilotFragment instance = null;
    private OnSeekBarMovedListener mlistener;
    private TextView mDistanceTextView, mAngleTextView, mAltitudeTextView;
    private int angle;
    private float rayon, altitude;


    public AutopilotFragment() {
        // Required empty public constructor
    }

    public interface OnSeekBarMovedListener {
        public void onSeekBarMoved(int seekBarID, int value);
    }

    /**
     * Use this  method to get the singleton instance of
     * this fragment using the provided parameters.
     *
     * @return The instance of fragment ManualControlFragment.
     */

    public static AutopilotFragment getInstance() {
        if (instance == null){
            instance = new AutopilotFragment();
        }
        return instance;
    }

    public void setDistance(float distance){
        rayon = distance;
        mDistanceTextView.setText(String.valueOf(distance));
    }

    public void setAngle(int angle){
        this.angle = angle;
        mAngleTextView.setText(String.valueOf(angle));
    }

    public void setAltitude(float altitude){
        this.altitude = altitude;
        mAltitudeTextView.setText(String.valueOf(altitude));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDistanceSeekBar = getActivity().findViewById(R.id.distanceSeekBar);
        mAngleSeekBar = getActivity().findViewById(R.id.angleSeekBar);
        mAltitudeSeekBar = getActivity().findViewById(R.id.altitudeSeekBar);

        mDistanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b){
                    mlistener.onSeekBarMoved(DISTANCE, i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mAngleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    mlistener.onSeekBarMoved(ANGLE, i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mAltitudeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b){
                    mlistener.onSeekBarMoved(ALTITUDE, i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mDistanceTextView = getActivity().findViewById(R.id.distanceTextValue);
        mAngleTextView = getActivity().findViewById(R.id.angleTextValue);
        mAltitudeTextView = getActivity().findViewById(R.id.altitudeTextValue);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_autopilot, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mlistener = (OnSeekBarMovedListener) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAngleTextView.setText(String.valueOf(angle));
        mDistanceTextView.setText(String.valueOf(rayon));
        mAltitudeTextView.setText(String.valueOf(altitude));
    }
}