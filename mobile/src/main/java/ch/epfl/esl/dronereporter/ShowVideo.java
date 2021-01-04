package ch.epfl.esl.dronereporter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class ShowVideo extends AppCompatActivity {


    private RecyclerView recyclerView;
    //private RecyclerViewAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_video);

        recyclerView=findViewById(R.id.recyclerView_ShowVideo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

       //recyclerViewAdapter= new RecyclerViewAdapter();

       // recyclerView.setAdapter(recyclerViewAdapter);

    }
}