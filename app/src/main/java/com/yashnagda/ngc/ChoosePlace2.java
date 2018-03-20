package com.yashnagda.ngc;

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
import android.widget.EditText;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChoosePlace2 extends AppCompatActivity {
    private Button button;
    EditText textView;
    private DatabaseReference mDatabaseReference;
    public String  id;
    SearchableSpinner mspinner;
    List<String> namesList = new ArrayList<String>();
    public String mcity;
    private DatabaseReference mReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = getSharedPreferences("ActivityPREF", Context.MODE_PRIVATE);
        if(pref.getBoolean("activity_executed", false)){
            Intent intent = new Intent(this, Main2Activity.class);
            startActivity(intent);
            finish();
        } else {
            SharedPreferences.Editor ed = pref.edit();
            ed.putBoolean("activity_executed", true);
            ed.commit();
        }
        setContentView(R.layout.activity_choose_place2);
        textView= (EditText) findViewById(R.id.invisible);
        button=(Button)findViewById(R.id.city_button);
        mspinner = (SearchableSpinner) findViewById(R.id.city_spinner);
        id=getIntent().getExtras().get("state").toString();
        textView.setText(id);
        mDatabaseReference= FirebaseDatabase.getInstance().getReference().child("places").child(id);
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long id= dataSnapshot.getValue(long.class);
                textView.setText(String.valueOf(id));

                if(isNetworkAvailable()) {
                    String url = "https://www.whizapi.com/api/v2/util/ui/in/indian-city-by-state?stateid="+id+"&project-app-key=z8dykvkyfbisejnqngekydee";

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
                                            namesList.add(state_list.getJSONObject(i).getString("city"));
                                        }

                                        //String[] array = namesList.toArray(new String[0]);

                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ChoosePlace2.this, android.R.layout.simple_dropdown_item_1line, namesList);
                                        mspinner.setAdapter(adapter);
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city=mspinner.getSelectedItem().toString();
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String uid=auth.getCurrentUser().getUid();
                String email=auth.getCurrentUser().getEmail();
                String name=auth.getCurrentUser().getDisplayName();
                mReference=FirebaseDatabase.getInstance().getReference().child("user_profile");
                DatabaseReference newuser=mReference.child(uid);
                newuser.child("email").setValue(email);
                newuser.child("name").setValue(name);
                newuser.child("state").setValue(id);
                newuser.child("city").setValue(city);
                newuser.child("state_city").setValue(id+"_"+city);
                startActivity(new Intent(ChoosePlace2.this,Main2Activity.class));
            }
        });



    }

    public void showSnackbar(String king) {
        AlertDialog.Builder builder=new AlertDialog.Builder(ChoosePlace2.this);
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
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
