package ch.epfl.esl.dronereporter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import io.grpc.Context;

public class MainVideo extends AppCompatActivity {

    private static final int PICK_VIDEO=1;
    VideoView videoView;
    Button button;
    EditText editText;
    Button ShowVideosButton;

    private Uri videoUri;
    MediaController mediaController;

    StorageReference storageReference;
    DatabaseReference databaseReference;
    MemberVideo memberVideo;
    UploadTask uploadTask;

    FirebaseAuth mAuth;

    private final String TAG ="error_finding";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video);

        memberVideo = new MemberVideo();

        storageReference= FirebaseStorage.getInstance().getReference("Video");
        databaseReference= FirebaseDatabase.getInstance().getReference("video");

        videoView= findViewById(R.id.video_view_main);
        button=findViewById(R.id.button_upload_main);
        ShowVideosButton=findViewById(R.id.ButtonGoToVideo);

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

            store_video(data);
            videoUri=data.getData();
            videoView.setVideoURI(videoUri);
        }
    }

    private void store_video(Intent data) {

        final String videoName=editText.getText().toString();

        final String search= editText.getText().toString().toLowerCase();

        try{
            File newfile;

            AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
            FileInputStream in = videoAsset.createInputStream();

            File filepath = Environment.getExternalStorageDirectory();
            File dir = new File(filepath.getAbsolutePath() + "/Drone Reporter/Videos");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            newfile = new File(dir, videoName + ".mp4");

            if (newfile.exists()) newfile.delete();

            OutputStream out = new FileOutputStream(newfile);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();
            Log.i(TAG, "Copy file successful.");

        }
        catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "copy NOT successful");
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



    //Permission for storage and all
    private int STORAGE_PERMISSION_CODE=1;

    public void show_video(View view) {
        /*
        Intent intent = new Intent(this, ShowVideo.class);
        Log.i(TAG, "Moving to video views");

        startActivity(intent);
         */

        if(ContextCompat.checkSelfPermission(MainVideo.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(MainVideo.this,"You already gave the permission to write in storage",Toast.LENGTH_SHORT).show();
        }

        else{
            requestStoragePermission();
        }
    }

    private void requestStoragePermission(){
        //  if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

           /* new AlertDialog.Builder(this),setTitle("permission needed").setMessage(
                    ""
            )*/
        //}

        // else
        //{
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
        //}

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode==STORAGE_PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                                Toast.makeText(MainVideo.this, "Data saved", Toast.LENGTH_SHORT).show();

                                memberVideo.setName(videoName);
                                memberVideo.setVideourl(downloadUri.toString());
                                memberVideo.setSearch(search);

                                String i= databaseReference.push().getKey();

                                databaseReference.child(i).setValue(memberVideo);

                            }
                            else{
                                Toast.makeText(MainVideo.this, "Failed", Toast.LENGTH_SHORT).show();
                            }


                        }
                    });

        }

    }


    private StorageReference mStorageRef;

    public void pull_video(View view) {

        String query="foobar";
        Query firebaseQuery= databaseReference.orderByChild("search").startAt(query).endAt(query+ "\uf8ff");

        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot data:dataSnapshot.getChildren()){

                    String output=data.getValue().toString();
                    Log.v(TAG, "Sucessful search: "+output);
                    //Records models=data.getValue(Records.class);
                    //String latitude=models.getLatitude();
                    //String longitude=models.getLongitude();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v(TAG, "Failed search");

            }
        });

        // String videourl = // dataSnapshot.child("photo").getValue(String.class);

        //StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videourl);

/*
       // StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl
         //       (photo);
*/

/*
        videoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
             //   download_video();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("foobar", "Cannot write because:", e);
                e.printStackTrace();
            }
        });

*/
        File filepath = Environment.getExternalStorageDirectory();
        File localFile = new File(filepath.getAbsolutePath() + "/Drone Reporter/Videos");
        if (!localFile.exists()) {
            localFile.mkdirs();
        }

        File newfile = new File(localFile, "bay_area_wallpaper" + ".jpg");

        if (newfile.exists()) newfile.delete();

        StorageReference storageRef =FirebaseStorage.getInstance().getReference().child("bay_area_wallpaper.jpg");

        storageRef.getFile(newfile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.v(TAG, "successful download");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.v(TAG, "failed download");
            }});

    }
}