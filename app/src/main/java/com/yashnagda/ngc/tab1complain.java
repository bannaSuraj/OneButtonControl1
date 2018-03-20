package com.yashnagda.ngc;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Yash Nagda on 3/19/2017.
 */

public class tab1complain extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final long LOCATION_REFRESH_TIME = 100;
    private static final long LOCATION_REFRESH_DISTANCE = 100;
    private static final String TAG ="yash" ;
    EditText et1, et2;
    ImageView bt;
    Button button;
    public static final int GALLERY_REQEST = 10;
    private StorageReference mStorageRef;
    ProgressDialog progressDialog;
    Uri imageuri=null;
    private DatabaseReference mDatabaseReference;
    private String city="yash",country="yash";
    private double  Latitude=0, Longitude=0;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    // directory name to store captured images
    private static final String IMAGE_DIRECTORY_NAME = "OBC Camera";
    private Uri fileUri; // file url to store image/video
    private RequestQueue requestQueue;




    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters
    boolean gps_enabled = false;
    boolean network_enabled = false;
    String address="";
    Uri mUri;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1complain, container, false);
        //GPSTracker gpsTracker=new GPSTracker(getActivity());
        progressDialog = new ProgressDialog(getActivity());
        mDatabaseReference= FirebaseDatabase.getInstance().getReference().child("users");
        mStorageRef= FirebaseStorage.getInstance().getReference();

        button = (Button) rootView.findViewById(R.id.obcbutton);
        et1 = (EditText) rootView.findViewById(R.id.obctext);
        bt = (ImageView) rootView.findViewById(R.id.obcimage);
        requestQueue= Volley.newRequestQueue(getActivity());

        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();
        }

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String [] items = new String[] {"Camera","Gallery"};
                final Integer[] icons = new Integer[] {R.mipmap.vip_ic_linked_camera, R.mipmap.vip_gallery_icon};
                ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);

                new AlertDialog.Builder(getActivity()).setTitle("Select Image")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item ) {
                                if(item==0)
                                {
                                    captureImage();

                                }else {
                                    Intent galleryintent = new Intent(Intent.ACTION_GET_CONTENT);
                                    galleryintent.setType("image/*");
                                    startActivityForResult(galleryintent, GALLERY_REQEST);
                                }
                            }
                        }).show();


            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                uploadimage();
                /*Intent intent = new Intent(getActivity(), message.class);
                startActivity(intent);*/

            }
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_REQEST && resultCode == RESULT_OK) {
            imageuri = data.getData();
            bt.setImageURI(imageuri);

        }

        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // successfully captured the image
                // display it in image view
                previewCapturedImage();
                //fileUri=data.getData();
                //bt.setImageURI(imageuri);


            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getActivity(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getActivity(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void uploadimage() {
       /* if(Longitude==0||Latitude==0||city.equals("")||country.equals("")){
            mynetwork();
            uploadimage();
        }else {*/

        final String st1;
        st1 = et1.getText().toString();
        if (st1.equals("")) {
            showSnackbar("Please fill the text field...");
        } else if (imageuri == null && mUri==null) {
            showSnackbar("Please select the image...");
        } else {
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            if(imageuri!=null) {
                StorageReference riversRef = mStorageRef.child("images").child(imageuri.getLastPathSegment());
                riversRef.putFile(imageuri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                FirebaseAuth auth = FirebaseAuth.getInstance();
                        /*String name = auth.getCurrentUser().getDisplayName();
                        String email = auth.getCurrentUser().getEmail();
                        Uri photoUrl = auth.getCurrentUser().getPhotoUrl();*/
                                //String uid = auth.getCurrentUser().getUid();
                                String uid = auth.getCurrentUser().getUid().toString();
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                final DatabaseReference newpost = mDatabaseReference.push();
                                newpost.child("name").setValue(st1);
                                newpost.child("profile_image").setValue(downloadUrl.toString());
                                newpost.child("uid").setValue(uid);
                                newpost.child("longitude").setValue(Longitude);
                                newpost.child("latitude").setValue(Latitude);
                                //newpost.child("city").setValue(city);
                                //newpost.child("country").setValue(country);
                                DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference().child("user_profile").child(uid);
                                databaseReference.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        user_profile up=dataSnapshot.getValue(user_profile.class);
                                        newpost.child("state").setValue(up.getState());
                                        newpost.child("city").setValue(up.getCity());
                                        newpost.child("state_city").setValue(up.getState_city());

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                progressDialog.dismiss();
                                Toast.makeText(getActivity(), "SUCESSFULLY UPLOADED...", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(getActivity(), message.class));
                                getActivity().finish();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                                Toast.makeText(getActivity(), "ERROR IN UPLOADING...", Toast.LENGTH_LONG).show();
                            }
                        });
            }else {
                StorageReference riversRef = mStorageRef.child("images").child(fileUri.getLastPathSegment());
                riversRef.putFile(fileUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                FirebaseAuth auth = FirebaseAuth.getInstance();
                        /*String name = auth.getCurrentUser().getDisplayName();
                        String email = auth.getCurrentUser().getEmail();
                        Uri photoUrl = auth.getCurrentUser().getPhotoUrl();*/
                                //String uid = auth.getCurrentUser().getUid();
                                String uid = auth.getCurrentUser().getUid().toString();
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                final DatabaseReference newpost = mDatabaseReference.push();
                                newpost.child("name").setValue(st1);
                                newpost.child("profile_image").setValue(downloadUrl.toString());
                                newpost.child("uid").setValue(uid);
                                newpost.child("longitude").setValue(Longitude);
                                newpost.child("latitude").setValue(Latitude);
                                //setotherdata(newpost,uid);
                                //newpost.child("address").setValue(address);
                                //newpost.child("city").setValue(city);
                                //newpost.child("country").setValue(country);
                                DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference().child("user_profile").child(uid);
                                databaseReference.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        user_profile up=dataSnapshot.getValue(user_profile.class);
                                        newpost.child("state").setValue(up.getState());
                                        newpost.child("city").setValue(up.getCity());
                                        newpost.child("state_city").setValue(up.getState_city());

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                progressDialog.dismiss();
                                Toast.makeText(getActivity(), "SUCESSFULLY UPLOADED...", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(getActivity(), message.class));
                                getActivity().finish();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                // ...
                                Toast.makeText(getActivity(), "ERROR IN UPLOADING...", Toast.LENGTH_LONG).show();
                            }
                        });


            }
        }
    }

    public void showSnackbar(String king) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
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
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Capturing Camera Image will lauch camera app requrest image capture
     */
    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }
    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        button.onRestoreInstanceState(fileUri);

        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }
    /**
     * Receiving activity result method will be called after closing the camera
     * */
    private void previewCapturedImage() {
        try {

            bt.setVisibility(View.VISIBLE);

            // bimatp factory
            BitmapFactory.Options options = new BitmapFactory.Options();

            // downsizing image as it throws OutOfMemory Exception for larger
            // images
            options.inSampleSize = 8;

            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
                    options);

            bt.setImageBitmap(bitmap);
            mUri=getImageUri(getActivity(),bitmap);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");




        return mediaFile;
    }



    private void displayLocation() throws IOException {
        LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Latitude = mLastLocation.getLatitude();
            Longitude = mLastLocation.getLongitude();


                String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + Latitude + ","
                        + Longitude + "&key=AIzaSyCEh4B8DhYnokSMxFdqcbKaMYYFDunKyS0";

                JsonObjectRequest jsObjRequest = new JsonObjectRequest
                        (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    address = response.getJSONArray("results").getJSONObject(0).
                                            getString("formatted_address");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                //et1.setText("Response: " + address);
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO Auto-generated method stub
                                error.printStackTrace();

                                Log.d("Error = ", error.toString());
                                et1.setText("Error: " + error.toString());

                            }
                        });

// Access the RequestQueue through your singleton class.
                MySingleton.getInstance(getActivity()).addToRequestQueue(jsObjRequest);


        /*    RequestQueue queue = Volley.newRequestQueue(getActivity());
            String url ="https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + ","
                    + longitude + "&key=AIzaSyCEh4B8DhYnokSMxFdqcbKaMYYFDunKyS0";
// Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            et1.setText("Response is: "+ response);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    et1.setText("That didn't work!");
                }
            });
// Add the request to the RequestQueue.
            queue.add(stringRequest);*/


                //String address=mrg.getAddress();
          /*  Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(getActivity(), Locale.getDefault());

            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String citys = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String countrys = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();
            city=mrg.getAddress1();
            country=mrg.getState();

            et1.setText(mrg.getAddress1()+ ", " + longitude+citys + ", " + countrys+", "+address + ", " + state+postalCode + ", " + knownName);
*/

        }
    }

    /**
     * Creating google api client object
     * */
   protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                getActivity().finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkPlayServices();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    public void showSettingsAlert() {
        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(getActivity());
        alertDialog.setCancelable(false);
        //Setting Dialog Title
        alertDialog.setTitle(R.string.GPSAlertDialogTitle);

        //Setting Dialog Message
        alertDialog.setMessage(R.string.GPSAlertDialogMessage);
        // alertDialog.setIcon(R.drawable.gen);

        //On Pressing Setting button
        alertDialog.setPositiveButton(R.string.gps_action_settings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                //mContext.startActivity(new Intent(mContext, Post_activity.class));

                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getActivity().startActivity(intent);
            }
        });

        //On pressing cancel button
        alertDialog.setNegativeButton(R.string.gps_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                getActivity().finish();
                //mContext.startActivity(new Intent(mContext, Post_activity.class));
            }
        });

        alertDialog.show();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void setotherdata(final DatabaseReference newpost, String uid){
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference().child("user_profile").child(uid);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_profile up=dataSnapshot.getValue(user_profile.class);
                newpost.child("state").setValue(up.getState());
                newpost.child("city").setValue(up.getCity());
                newpost.child("state_city").setValue(up.getState_city());
                Toast.makeText(getActivity(), "result"+up.getCity()+up.getState()+up.getState_city(),
                        Toast.LENGTH_LONG).show();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
