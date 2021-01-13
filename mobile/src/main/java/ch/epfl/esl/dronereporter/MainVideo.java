package ch.epfl.esl.dronereporter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

public class MainVideo extends AppCompatActivity {

    private static final int PICK_VIDEO=1;
    private static final int PICTURE =0 ;
    private static final int VIDEO =1 ;
    private static final String MEDIA_PATH ="/DroneReporter/Videos";
    private static final String PHOTO_PATH="/DroneReporter/Photos";

    Button button;
    EditText editText;
    Button ShowVideosButton;

    private Uri MediaUri;

    private String mediaName;
    private String search;

    StorageReference storageReference;
    DatabaseReference databaseReference;
    MemberVideo memberMedia;
    UploadTask uploadTask;

    private final String TAG ="error_finding";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video);

        memberMedia = new MemberVideo();

        storageReference= FirebaseStorage.getInstance().getReference("Media");
        databaseReference= FirebaseDatabase.getInstance().getReference("media");

        // videoView= findViewById(R.id.video_view_main);
        //button=findViewById(R.id.button_upload_main);
        ShowVideosButton=findViewById(R.id.ButtonGoToVideo);


//Storage permission!
        if(ContextCompat.checkSelfPermission(MainVideo.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
        {
            //Toast.makeText(MainVideo.this,"You already gave the permission to write in storage",Toast.LENGTH_SHORT).show();
        }

        else{
            requestStoragePermission();
        }

    }


    public void StoreMedia(View view) {

        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_VIDEO);

    }

    public void showMedia(View view) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri myDir = Uri.parse(Environment.getExternalStorageDirectory().getPath()+ MEDIA_PATH);
        intent.setDataAndType(myDir,"*/*");    // or use */*
        startActivity(Intent.createChooser(intent, MEDIA_PATH));

    }


    public void sync(View view) {

        //Get a list of file names in local storage
        String path = Environment.getExternalStorageDirectory().toString()+ MEDIA_PATH;
        File directory = new File(path);
        final File[] localFiles = directory.listFiles();

        for (int i = 0; i < localFiles.length; i++)
        {
            Log.d(TAG, "FileName:" + localFiles[i].getName());
        }


        Query firebaseQuery= databaseReference.orderByChild("search");//.startAt(query).endAt(query+ "\uf8ff");
        Log.v(TAG, "Before the listener");

        firebaseQuery.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "Inside the listener");

                //String [] search= new String[(int) dataSnapshot.getChildrenCount()];
                for (DataSnapshot data:dataSnapshot.getChildren()){

                    boolean is_local= false;

                    String search_cloud=data.child("search").getValue(String.class);
                    Log.v(TAG, "Successful access to search value: "+ search_cloud);

                    for(int i=0; i<localFiles.length; i++)
                    {
                        Log.v(TAG, "Locally we have:"  +localFiles[i].getName());
                        Log.v(TAG, "In the cloud we have:"  + search_cloud);

                        if(search_cloud.equals(localFiles[i].getName())) {
                            is_local = true;
                            Log.v(TAG, "It's already in the local storage! " + search_cloud);
                            break;
                        }

                    }
                    if(is_local==false) {
                        Log.v(TAG, "I am about to download!" + search_cloud);
                        downloadData(search_cloud, data.child("videourl").getValue(String.class), VIDEO);
                    }
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v(TAG, "Failed access to search value");
            }
        });
    }











    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode==PICK_VIDEO || resultCode ==RESULT_OK )
                && (data != null) && (data.getData()!= null)){

            MediaUri =data.getData();

            storeMediaLocally(data);
            UploadMedia();
        }

        if((requestCode==SHOW_STORAGE || resultCode ==RESULT_OK )
                && (data != null) && (data.getData()!= null))
        {
            Log.v(TAG, "Went to see storage!!!");
        }
    }

    private void storeMediaLocally(Intent data) {

        mediaName =Long.toString(System.currentTimeMillis()) + "."+getExt(MediaUri) ;
        search= Long.toString(System.currentTimeMillis()) + "."+getExt(MediaUri);

        try{
            File newfile;

            AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
            FileInputStream in = videoAsset.createInputStream();

            File filepath = Environment.getExternalStorageDirectory();
            File dir = new File(filepath.getAbsolutePath() + MEDIA_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            newfile = new File(dir, mediaName);

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




    private String getExt(Uri uri)
    {
        ContentResolver contentResolver=getContentResolver();
        MimeTypeMap mimeTypeMap= MimeTypeMap.getSingleton();

        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    //Permission for storage and all
    private int STORAGE_PERMISSION_CODE=1;
    final private int SHOW_STORAGE=2;


    private void requestStoragePermission(){

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
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

    private void UploadMedia()
    {
        //   final String videoName=  //Long.toString(System.currentTimeMillis())+".mp4"  ;
        // final String search=    //Long.toString(System.currentTimeMillis()) +".mp4";

        if(MediaUri !=null || !TextUtils.isEmpty(mediaName))
        {
            final StorageReference reference = storageReference.child(System.currentTimeMillis()+"."+getExt(MediaUri));
            uploadTask=reference.putFile(MediaUri);

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

                                memberMedia.setName(mediaName);
                                memberMedia.setVideourl(downloadUri.toString());
                                memberMedia.setSearch(search);

                                String i= databaseReference.push().getKey();

                                databaseReference.child(i).setValue(memberMedia);

                            }
                            else{
                                Toast.makeText(MainVideo.this, "Failed", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

        }

    }


    private void downloadData(String name ,String videoURL, int dataType)
    {
        String pathName= MEDIA_PATH;
        if(dataType==PICTURE)
            pathName=PHOTO_PATH;

        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoURL);

        File filepath = Environment.getExternalStorageDirectory();
        File localFile = new File(filepath.getAbsolutePath() + pathName);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }


        File newfile = new File(localFile, name );//+ extension);


        if (newfile.exists()) newfile.delete();

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

    private void searchData(String query)
    {
        Query firebaseQuery= databaseReference.orderByChild("search").startAt(query).endAt(query+ "\uf8ff");
        Log.v(TAG, "Before the listener");

        firebaseQuery.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "Inside the listener");

                for (DataSnapshot data:dataSnapshot.getChildren()){

                    String videoURL=data.child("videourl").getValue(String.class);
                    Log.v(TAG, "Sucessful search: "+ videoURL);

                    String name=data.child("name").getValue(String.class);
                    downloadData( name, videoURL, VIDEO);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v(TAG, "Failed search");
            }
        });
    }



}