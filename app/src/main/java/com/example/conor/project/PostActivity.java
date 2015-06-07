package com.example.conor.project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;


public class PostActivity extends ActionBarActivity {

    private GoogleMap mMap;
    private double[] lastLocationArray;
    private LocationManager locationManager;
    private Criteria criteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Hide the action bar and set the color of the underline
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.getBackground().setColorFilter(Color.parseColor("#FF8F00"), PorterDuff.Mode.SRC_IN);

        // Location/map stuff
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        Intent intent = getIntent();
        lastLocationArray = intent.getDoubleArrayExtra("lastLocationArray");

        setUpMapIfNeeded();

        if(lastLocationArray != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocationArray[0], lastLocationArray[1]), (float) 18.5));



    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(lastLocationArray != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocationArray[0], lastLocationArray[1]), (float)18.5));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    ///////------ MAP STUFF ---------///////////////
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(false);

        Circle cirlce = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lastLocationArray[0], lastLocationArray[1]))
                .radius(50)
                .strokeColor(Color.parseColor("#FFA000"))
                .fillColor(Color.argb(30, 255, 222, 0))
                .strokeWidth(3));
    }

    /////////--------- END MAP STUFF ---------------//////////////////

    //clears the text from the editText when clicked
    public void clearText(View v){
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setText("");
        editText.setTextColor(Color.parseColor("#000000"));
    }

    // Function stub for when the post button is pressed
    public void clickPost(View v){
        EditText editText = (EditText) findViewById(R.id.editText);
        Editable textToPost = editText.getText();

    }
}
