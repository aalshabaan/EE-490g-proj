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

import com.google.android.gms.common.internal.Constants;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainVideo extends AppCompatActivity {

    private static final int DELETE_MEDIA=2;
    private static final int PICK_MEDIA =1;
    private static final int VIDEO =1 ;
    private static final String ABSOLUTE_PATH= "/DroneReporter/" ;
    private static final String MEDIA_PATH = "/DroneReporter/media/";
    private static  final String CLOUD_PATH = "/DroneReporter/cloud/";


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
    }

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

        Uri myDir = Uri.parse(Environment.getExternalStorageDirectory() + ABSOLUTE_PATH);
        Intent intent = new Intent(Intent.ACTION_VIEW,myDir);
        //  Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT_TREE,myDir);
        intent.setDataAndType(myDir, "*/*");

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

        //We first pull from the cloud the files that are not present in the local cloud file
        String path = Environment.getExternalStorageDirectory().toString()+ CLOUD_PATH;
        File directory = new File(path);
        final File[] cloudFiles = directory.listFiles();
        if (cloudFiles == null){
            Log.d(TAG, "sync: No Local files found!");
        }
        else {
            for (int i = 0; i < cloudFiles.length; i++) {
                Log.d(TAG, "FileName:" + cloudFiles[i].getName());
            }
        }
//This is where the transaction with firebase begin
        Query firebaseQuery= databaseReference.orderByChild("search");//.startAt(query).endAt(query+ "\uf8ff");
        Log.v(TAG, "Before the listener");
        Toast.makeText(this, "Synchronising with the cloud", Toast.LENGTH_SHORT).show();
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "Inside the listener");

                for (DataSnapshot data:dataSnapshot.getChildren()){

                    boolean is_local= false;

                    String search_cloud=data.child("search").getValue(String.class);
                    Log.v(TAG, "Successful access to search value: "+ search_cloud);

                    for(int i=0; i<cloudFiles.length; i++)
                    {
                        // Log.v(TAG, "Locally we have:"  +localFiles[i].getName());
                        // Log.v(TAG, "In the cloud we have:"  + search_cloud);

                        if(search_cloud.equals(cloudFiles[i].getName())) {
                            is_local = true;
                            // Log.v(TAG, "It's already in the local cloud storage! " + search_cloud);
                            break;
                        }

                    }
                    if(is_local==false) {
                        Log.v(TAG, "I am about to download!" + search_cloud);
                        // downloadData(search_cloud, data.child("videourl").getValue(String.class), MEDIA_PATH); //Sync with media folder
                        downloadData(search_cloud, data.child("videourl").getValue(String.class), CLOUD_PATH); //Sync with cloud folder
                    }
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v(TAG, "Failed access to search value");
            }
        });
//Our cloud has been updated, we then copy the files that are not in the media folder from the local cloud folder

        String mediaPath = Environment.getExternalStorageDirectory().toString()+ MEDIA_PATH;
        File mediaDirectory = new File(mediaPath);
        final File[] mediaFiles = mediaDirectory.listFiles();


        if (cloudFiles!=null){

            for (int i = 0; i < cloudFiles.length; i++) {
                boolean is_local= false;
                if(mediaFiles!=null) {

                    for (int j = 0; j < mediaFiles.length; j++) {
                        Log.d(TAG, "The media files is:"+mediaFiles[j].getName() );
                        Log.d(TAG, "The local file is:"+cloudFiles[i].getName() );

                        if (mediaFiles[j].getName().equals(cloudFiles[i].getName())) {
                            is_local= true;
                            break;
                        }
                        if(is_local==false){
                            Log.d(TAG, "I copied!"+cloudFiles[i].getName() );
                            copyFile(CLOUD_PATH, cloudFiles[i].getName(), MEDIA_PATH);
                        }
                    }
                }

            }
        }
    }


    private void copyFile(String i_inputPath, String inputFile, String i_outputPath) {

        InputStream in = null;
        OutputStream out = null;

        // Add absolute path to both paths!
        String inputPath= Environment.getExternalStorageDirectory().toString() +i_inputPath;
        String outputPath=Environment.getExternalStorageDirectory().toString()  +i_outputPath;

        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);

            // File newfile = new File(outputPath, inputFile);

            //if (newfile.exists()) newfile.delete();

            out = new FileOutputStream(outputPath + inputFile);
            //  out = new FileOutputStream(newfile);


            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file (You have now copied the file)
            out.flush();
            out.close();
            out = null;
            Log.i(TAG, "Successful copy!");

        }  catch (FileNotFoundException fnfe1) {
            Log.e(TAG, fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode== PICK_MEDIA && resultCode ==RESULT_OK )
                && (data != null) && (data.getData()!= null)){

            MediaUri =data.getData();

            Log.d(TAG, "onActivityResult: filename: " + getFileName(MediaUri));
            storeMediaLocally(data);
            UploadMedia();
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
        Toast.makeText(this, "About to delete the file", Toast.LENGTH_SHORT).show();
        firebaseQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //  Log.v(TAG, "Inside the listener");
                for (DataSnapshot data:dataSnapshot.getChildren()){

                    String search_cloud=data.child("search").getValue(String.class);

                    // Log.v(TAG, "Successful access to search value: "+ search_cloud);

                    if(search_cloud.equals(nameFile)){
                        Log.v(TAG, "Going to delete this: "+ search_cloud);
                        String storageUrl =data.child("videourl").getValue(String.class);
                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storageUrl);

                        //Remove from the storage first using the url in the database
                        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // File deleted successfully
                                Log.d(TAG, "onSuccess: deleted file from cloud storage");
                                //Toast.makeText(this, "About to delete the file", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
                                Log.d(TAG, "onFailure: did not delete file from cloud storage");
                            }
                        });

                        data.getRef().removeValue(); //Then we remove value from database!

                        //deleteFile(Environment.getExternalStorageDirectory().getPath()+CLOUD_PATH+ nameFile);
                        try {
                            File file = new File(Environment.getExternalStorageDirectory().getPath() + CLOUD_PATH + nameFile);
                            boolean deleted = file.delete();
                            Log.i(TAG, "The file was  also deleted locally !");

                        }
                        catch (Exception e)
                        {
                            Log.i(TAG, "Could not delete the file!");
                        }
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

        Log.d(TAG, "I am calling upload media!");

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
                                Log.v(TAG,"Data pushed to cloud");

                                memberMedia.setName(mediaName);
                                memberMedia.setVideourl(downloadUri.toString());
                                memberMedia.setSearch(search);

                                String i= databaseReference.push().getKey();

                                databaseReference.child(i).setValue(memberMedia);

                            }
                            else{
                                Toast.makeText(MainVideo.this, "Failed", Toast.LENGTH_SHORT).show();
                                Log.v(TAG,"Failed to push to cloud");

                            }

                        }
                    });

        }

    }


    private void downloadData(String name ,String videoURL, String storageLocation)
    {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoURL);
        File filepath = Environment.getExternalStorageDirectory();

        //First we download in the media path!

        File localFile = new File(filepath.getAbsolutePath() + storageLocation);
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
                    downloadData( name, videoURL, MEDIA_PATH);
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
