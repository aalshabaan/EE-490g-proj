package ch.epfl.esl.dronereporter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    public final String PRESENTER_MODE = "presenter";
    public final String DOCUMENTER_MODE = "documenter";
    public final String MODE = "mode";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void engageDrone(View view){
        String mode = "";
        switch (view.getId()){
            case R.id.presenterModeButton:
                mode = PRESENTER_MODE;
                break;
            case R.id.documenterModeButton:
                mode = DOCUMENTER_MODE;
                break;
            default:
                Toast.makeText(this, "Unknown operation mode", Toast.LENGTH_SHORT).show();
        }
        Intent i = new Intent(this, DeviceListActivity.class);
        i.putExtra(MODE, mode);
        startActivity(i);

    }

    public void reviewFootage(View view){
        Intent i = new Intent(this, main_video.class);
        startActivity(i);
    }
}