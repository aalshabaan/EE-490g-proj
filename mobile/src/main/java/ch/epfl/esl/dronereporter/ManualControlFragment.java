package ch.epfl.esl.dronereporter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ManualControlFragment#getInstance method to
 * get the signleton instance of this fragment.
 */
public class ManualControlFragment extends Fragment {
    private static final String  TAG = "ManualControlFragment";

    public static final int ROLL = 0;
    public static final int YAW = 1;

    private static ManualControlFragment instance = null;
    private OnJoystickMovedListener mListener;
    private JoystickView mRollJoystick, mYawJoystick;
    public ManualControlFragment() {
        // Required empty public constructor
    }

    public interface OnJoystickMovedListener{
        void onJoystickMoved(int joystickID, int magnitude, int angle);
    }

    /**
     * Use this  method to get the singleton instance of
     * this fragment using the provided parameters.
     *
     * @return The instance of fragment ManualControlFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ManualControlFragment getInstance() {
        if (instance == null){
            instance = new ManualControlFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_manual_control, container, false);

    }

    @Override
    public void onStart() {
        super.onStart();
        mRollJoystick = getActivity().findViewById(R.id.rollJoystick);
        mYawJoystick = getActivity().findViewById(R.id.yawJoystick);
        Log.d(TAG, "onCreateView: " + mRollJoystick);

        mRollJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mListener.onJoystickMoved(ROLL, strength, angle);
            }
        });

        mYawJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mListener.onJoystickMoved(YAW, strength, angle);
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (OnJoystickMovedListener) context;
    }



}