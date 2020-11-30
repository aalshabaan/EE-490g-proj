package ch.epfl.esl.dronereporter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.myapplication.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class main_video extends AppCompatActivity {

    private static final int PICK_VIDEO=1;
    VideoView videoView;
    Button button;
    //ProgressBar progressBar;
    EditText editText;

    private Uri videoUri;
    MediaController mediaController;

    StorageReference storageReference;
    DatabaseReference databaseReference;
    Member_video memberVideo;
    UploadTask uploadTask;



   private final String TAG ="error_finding";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video);
       // Log.i(TAG, "I am creating the activity");

        memberVideo = new Member_video();

        storageReference= FirebaseStorage.getInstance().getReference("Video");
        databaseReference= FirebaseDatabase.getInstance().getReference("video");

        videoView= findViewById(R.id.video_view_main);
        button=findViewById(R.id.button_upload_main);
        //progressBar =findViewById(R.id.progressBar_main);

        editText=findViewById(R.id.et_video_name);

        mediaController= new MediaController (this);

        videoView.setMediaController(mediaController);
        videoView.start();



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadVideo();
            }
        });
}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PICK_VIDEO || resultCode ==RESULT_OK ||
        data != null || data.getData()!= null){
            videoUri=data.getData();
            videoView.setVideoURI(videoUri);
        }
    }

    public void choose_video(View view) {

        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_VIDEO);

    }


    private String getExt(Uri uri)
    {
        ContentResolver contentResolver=getContentResolver();

        MimeTypeMap mimeTypeMap= MimeTypeMap.getSingleton();


        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }



    public void show_video(View view) {
    }


    private void UploadVideo()
    {

        final String videoName=editText.getText().toString();

        final String search= editText.getText().toString().toLowerCase();

        if(videoUri!=null || !TextUtils.isEmpty(videoName))
        {
            final StorageReference reference = storageReference.child(System.currentTimeMillis()+"."+getExt(videoUri));
            uploadTask=reference.putFile(videoUri);

            Task<Uri> uritask=uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                   if(!task.isSuccessful()){
                       throw task.getException();
                   }

                    return reference.getDownloadUrl();
                }
            })

                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                         if(task.isSuccessful())
                         {
                             Uri downloadUri=task.getResult();
                             Toast.makeText(main_video.this, "Data saved", Toast.LENGTH_SHORT).show();

                             memberVideo.setName(videoName);
                             memberVideo.setVideourl(downloadUri.toString());
                             memberVideo.setSearch(search);

                             String i= databaseReference.push().getKey();

                             databaseReference.child(i).setValue(memberVideo);

                         }
                         else{
                             Toast.makeText(main_video.this, "Failed", Toast.LENGTH_SHORT).show();
                         }


                        }
                    });


        }


    }

}