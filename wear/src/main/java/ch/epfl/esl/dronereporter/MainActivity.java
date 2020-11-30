package ch.epfl.esl.dronereporter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    private TextView mTextView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        //mTextView.setText("Hello round World");

        //TextView textView=(R.id.w_display_message);

        // Enables Always-on
        setAmbientEnabled();
    }
}