package com.yashnagda.onebuttoncontrol;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class MainActivity extends AppCompatActivity {
    private static final long LOCATION_REFRESH_TIME = 100;
    private static final long LOCATION_REFRESH_DISTANCE = 100;
    EditText et1, et2;
    ImageButton bt;
    Button button;
    public static final int GALLERY_REQEST = 1;
    private StorageReference mStorageRef;
    ProgressDialog progressDialog;
    Uri imageuri=null;
    private DatabaseReference mDatabaseReference;
    private String city="",country="";
    private double Latitude=0, Longitude=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mynetwork();
        mDatabaseReference= FirebaseDatabase.getInstance().getReference().child("users");
        mStorageRef=FirebaseStorage.getInstance().getReference();
        progressDialog = new ProgressDialog(this);
        button = (Button) findViewById(R.id.login_bt);
        et1 = (EditText) findViewById(R.id.et1);
        et2 = (EditText) findViewById(R.id.et2);
        bt = (ImageButton) findViewById(R.id.imageView);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryintent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryintent.setType("image/*");
                startActivityForResult(galleryintent, GALLERY_REQEST);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //uploadimage();
                startActivity(new Intent(MainActivity.this,Main2Activity.class));

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQEST && resultCode == RESULT_OK) {
            imageuri = data.getData();
            bt.setImageURI(imageuri);

        }
    }

    public void uploadimage() {
       /* if(Longitude==0||Latitude==0||city.equals("")||country.equals("")){
            mynetwork();
            uploadimage();
        }else {*/
            final String st1, st2;
            et1 = (EditText) findViewById(R.id.et1);
            et2 = (EditText) findViewById(R.id.et2);
            st1 = et1.getText().toString();
            st2 = et2.getText().toString();
            if (st1.equals("") || st2.equals("")) {
                showSnackbar("Please fill all the fields...");
            } else if (imageuri == null) {
                showSnackbar("Please select the image...");
            } else {
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                StorageReference riversRef = mStorageRef.child("images").child(imageuri.getLastPathSegment());

                riversRef.putFile(imageuri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                //FirebaseAuth auth = FirebaseAuth.getInstance();
                        /*String name = auth.getCurrentUser().getDisplayName();
                        String email = auth.getCurrentUser().getEmail();
                        Uri photoUrl = auth.getCurrentUser().getPhotoUrl();*/
                                //String uid = auth.getCurrentUser().getUid();
                                String uid="1";
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                DatabaseReference newpost = mDatabaseReference.push();
                                newpost.child("name").setValue(st1);
                                newpost.child("bio").setValue(st2);
                                newpost.child("profile_image").setValue(downloadUrl.toString());
                                /*newpost.child("uid").setValue(uid);
                                newpost.child("longitude").setValue(Longitude);
                                newpost.child("latitude").setValue(Latitude);
                                newpost.child("city").setValue(city);
                                newpost.child("country").setValue(country);
*/
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "SUCESSFULLY UPLOADED...", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(MainActivity.this, message.class));

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                                Toast.makeText(MainActivity.this, "ERROR IN UPLOADING...", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }

    public void showSnackbar(String king) {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(king);
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void mynetwork(){
        GPSTracker gpsTracker = new GPSTracker(this);
        if(!(isNetworkAvailable())){

            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("No internet connection !!");
            builder.setCancelable(false);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            AlertDialog alertDialog=builder.create();
            alertDialog.show();


        } else if (gpsTracker.getIsGPSTrackingEnabled()) {


            Latitude = gpsTracker.latitude;
            //et1.setText(stringLatitude);

            Longitude = gpsTracker.longitude;
            //et2.setText(stringLongitude);


            country = gpsTracker.getCountryName(this);
            //et1.setText(country);

            city = gpsTracker.getLocality(this);
            //et2.setText(city);

            //String postalCode = gpsTracker.getPostalCode(this);
            //et1.setText(postalCode);

            //String addressLine = gpsTracker.getAddressLine(this);
            //et2.setText(addressLine);
        }
        else
        {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }

    }


}