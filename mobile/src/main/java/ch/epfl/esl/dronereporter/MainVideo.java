package ch.epfl.esl.dronereporter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainVideo extends AppCompatActivity {

    private static final int DELETE_MEDIA=2;
    private static final int PICK_MEDIA =1;
    // private static final int PICTURE =0 ;
    private static final int VIDEO =1 ;
    private static final String ABSOLUTE_PATH= "/DroneReporter/" ;
    private static final String MEDIA_PATH = "/DroneReporter/media";
    private static  final String CLOUD_PATH = "/DroneReporter/cloud";


    //Button ShowVideosButton;

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

        // ShowVideosButton=findViewById(R.id.ButtonGoToVideo);

//Storage permission!
        if(ContextCompat.checkSelfPermission(MainVideo.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
        {
            //Toast.makeText(MainVideo.this,"You already gave the permission to write in storage",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onCreate: We have external storage permission");
        }

        else{
            requestStoragePermission();
        }
        //createLocalFiles();

    }
/*
    private void createLocalFiles()
    {
        File filepath = Environment.getExternalStorageDirectory();

        File cloudDir = new File(filepath.getAbsolutePath() + CLOUD_PATH );

        if (!cloudDir.exists()) {
            if (!cloudDir.mkdirs())
                Log.e(TAG, "CreateLocalFiles: Unable to create cloud folder!");;
        }

        File dir = new File(filepath.getAbsolutePath() + MEDIA_PATH );

        if (!dir.exists()) {
            if (!dir.mkdirs())
                Log.e(TAG, "CreateLocalFiles: Unable to create media folder!!");;
        }

    }
*/


    public void deleteCloud(View view) {
        Intent intent = new Intent();
        Uri myDir = Uri.parse(Environment.getExternalStorageDirectory().getPath()+ CLOUD_PATH);
        Log.d(TAG, "Deleting something from the cloud" + myDir.getPath());
        intent.setDataAndType(myDir,"*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,DELETE_MEDIA);


    }


    public void StoreMedia(View view) {

        Intent intent = new Intent();
        Uri myDir = Uri.parse(Environment.getExternalStorageDirectory().getPath()+ MEDIA_PATH);
        Log.d(TAG, "StoreMedia: PATH in StoreMedia" + myDir.getPath());
        intent.setDataAndType(myDir,"*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_MEDIA);
    }

    public void showMedia(View view) {
/*
        Uri myDir = Uri.parse("file://"+Environment.getExternalStorageDirectory().getPath()+ MEDIA_PATH);
        Intent intent = new Intent(Intent.ACTION_VIEW,myDir);
        Log.i(TAG, "showMedia: PATH:" + myDir);
        try{
            startActivity(intent);

        }
        catch (Exception e){
            e.printStackTrace();
            Log.i(TAG, "copy NOT successful");
            }
 */
        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + ABSOLUTE_PATH);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
        {
            Log.i(TAG, "About to start the activity!");
            startActivity(intent);
        }
        else
        {
            Log.i(TAG, "Could not show the storage!");

        }





    }



    public void sync(View view) {

        //Get a list of file names in local storage
        String path = Environment.getExternalStorageDirectory().toString()+ MEDIA_PATH;
        File directory = new File(path);
        final File[] localFiles = directory.listFiles();
        if (localFiles == null){
            Log.d(TAG, "sync: No Local files found!");
        }
        else {
            for (int i = 0; i < localFiles.length; i++) {
                Log.d(TAG, "FileName:" + localFiles[i].getName());
            }
        }

        Query firebaseQuery= databaseReference.orderByChild("search");//.startAt(query).endAt(query+ "\uf8ff");
        Log.v(TAG, "Before the listener");
        Toast.makeText(this, "Synchronising with the cloud", Toast.LENGTH_SHORT).show();
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

        if((requestCode== PICK_MEDIA && resultCode ==RESULT_OK )
                && (data != null) && (data.getData()!= null)){

            MediaUri =data.getData();
            Log.d(TAG, "onActivityResult: filename: " + getFileName(MediaUri));
            UploadMedia();
            storeMediaLocally(data);
        }

        if((requestCode==SHOW_STORAGE && resultCode ==RESULT_OK )
                && (data != null) && (data.getData()!= null))
        {
            Log.v(TAG, "Went to see storage!!!");
        }

        if((requestCode==DELETE_MEDIA && resultCode ==RESULT_OK && (data != null) && (data.getData()!= null)))
        {
            Log.v(TAG, "We may delete something! ");
            MediaUri =data.getData();
            delete(getFileName(MediaUri));
            Log.d(TAG, "onActivityResult: filename: " + getFileName(MediaUri));

        }

    }

    private void delete(String nameFile)
    {
        Log.v(TAG, "I want to delete this: "+ nameFile);
        Query firebaseQuery= databaseReference.orderByChild("search");
        Log.v(TAG, "Before the listener");
        Toast.makeText(this, "About to delete the file", Toast.LENGTH_SHORT).show();
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "Inside the listener");
                for (DataSnapshot data:dataSnapshot.getChildren()){

                    String search_cloud=data.child("search").getValue(String.class);

                    Log.v(TAG, "Successful access to search value: "+ search_cloud);

                    if(search_cloud.equals(nameFile)){
                        Log.v(TAG, "Successful access to search value: "+ search_cloud);
                        String storageUrl =data.child("videourl").getValue(String.class);
                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storageUrl);

                        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully
                                Log.d(TAG, "onSuccess: deleted file from cloud storage");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
                                Log.d(TAG, "onFailure: did not delete file from cloud storage");
                            }
                        });

                        data.getRef().removeValue(); //This works, removes value from database!
                        // myFile.delete()
                    }

                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v(TAG, "Failed access to search value");
            }
        });
    }



    private void storeMediaLocally(Intent data) {

        mediaName = getFileName(MediaUri);
        Log.d(TAG, "storeMediaLocally: PATH" + MediaUri.getPath());

        Log.d(TAG, "storeMediaLocally: Filename" + mediaName);
        search= mediaName;

        try{
            File newfile;

            AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
            FileInputStream in = videoAsset.createInputStream();

            File filepath = Environment.getExternalStorageDirectory();
            // String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            //File dir = new File(filepath.getAbsolutePath() + MEDIA_PATH + today + File.separator);
            File dir = new File(filepath.getAbsolutePath() + CLOUD_PATH );//+ today + File.separator);

            if (!dir.exists()) {
                if (!dir.mkdirs())
                    Log.e(TAG, "storeMediaLocally: Unable to create folder!");;
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

        if(MediaUri !=null && !TextUtils.isEmpty(mediaName))
        {

            final StorageReference reference = storageReference.child(mediaName);
            Log.d(TAG, "UploadMedia: Filename: " + mediaName);
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

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


}