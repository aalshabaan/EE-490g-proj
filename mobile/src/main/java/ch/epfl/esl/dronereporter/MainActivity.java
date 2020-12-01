package ch.epfl.esl.dronereporter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {


    public final String PRESENTER_MODE = "presenter";
    public final String DOCUMENTER_MODE = "documenter";
    public final String MODE = "mode";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void engagePresenterMode(View view){
        Intent i = new Intent(this, DeviceListActivity.class);
        i.putExtra(MODE, PRESENTER_MODE);
        startActivity(i);
    }

    public void engageDocumenterMode(View view){
        Intent i = new Intent(this, DeviceListActivity.class);
        i.putExtra(MODE, DOCUMENTER_MODE);
        startActivity(i);
    }

    public void reviewFootage(View view){
        Intent i = new Intent(this, main_video.class);
        startActivity(i);
    }
}