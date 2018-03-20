package com.yashnagda.onebuttoncontrol;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import    com.toptoche.searchablespinnerlibrary.SearchableSpinner;

public class ChoosePlace extends AppCompatActivity {
    private DatabaseReference mDatabaseReference;
    List<String> namesList = new ArrayList<String>();
    List<Integer> idList = new ArrayList<Integer>();
    SearchableSpinner mspinner;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = new PrefManager(this);
        if (!prefManager.isFirstTimeLaunch()) {
            launchHomeScreen();
            finish();
        }
        setContentView(R.layout.activity_choose_place);
        mDatabaseReference= FirebaseDatabase.getInstance().getReference().child("places");
        if(isNetworkAvailable()) {
            String url = "https://www.whizapi.com/api/v2/util/ui/in/indian-states-list?project-app-key=z8dykvkyfbisejnqngekydee";

            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String arraySpinner[];
                                arraySpinner = new String[]{
                                        "1", "2", "3", "4", "5"
                                };
                                JSONArray state_list;
                                state_list = response.getJSONArray("Data");
                                for (int i = 0; i < state_list.length(); i++) {
                                    namesList.add(state_list.getJSONObject(i).getString("Name"));
                                    idList.add(state_list.getJSONObject(i).getInt("ID"));
                                }
                                mspinner = (SearchableSpinner) findViewById(R.id.state_spinner);
                                //String[] array = namesList.toArray(new String[0]);

                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(ChoosePlace.this, android.R.layout.simple_dropdown_item_1line, namesList);
                                mspinner.setAdapter(adapter);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //et1.setText("Response: " + address);
                            for(int i=0;i<namesList.size();i++){
                                mDatabaseReference.child(namesList.get(i)).setValue(idList.get(i));
                            }

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            error.printStackTrace();
                            Log.d("Error = ", error.toString());
                        }
                    });
            RequestQueue mRequestQueue;

// Instantiate the cache
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

// Set up the network to use HttpURLConnection as the HTTP client.
            Network network = new BasicNetwork(new HurlStack());

// Instantiate the RequestQueue with the cache and network.
            mRequestQueue = new RequestQueue(cache, network);

// Start the queue
            mRequestQueue.start();
            mRequestQueue.add(jsObjRequest);


        }else{
            showSnackbar("Please turn on your data connection !!");
        }


        Button button=(Button)findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state="";
                state=mspinner.getSelectedItem().toString();
                if(!state.equals("")) {

                    Intent intent = new Intent(ChoosePlace.this, ChoosePlace2.class);
                    intent.putExtra("state",state);
                    startActivity(intent);
                    finish();
                } else{
                    showSnackbar("Please select your city");
                }

            }
        });



    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showSnackbar(String king) {
        AlertDialog.Builder builder=new AlertDialog.Builder(ChoosePlace.this);
        builder.setMessage(king);
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }
    private void launchHomeScreen() {
        prefManager.setFirstTimeLaunch(false);
        startActivity(new Intent(ChoosePlace.this, Main2Activity.class));
        finish();
    }
}
