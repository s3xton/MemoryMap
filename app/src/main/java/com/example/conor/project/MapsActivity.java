package com.example.conor.project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity
    implements GoogleMap.OnMapClickListener{

    private static final String LOG_TAG = "MemoryMap";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private Location lastLocation;
    private Criteria criteria;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        lastLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        if(lastLocation != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)18.5));


        // Draw all of the messages pulled from server. (so far using dummy info)
        double[] a = {37.001816, -122.057976};
        int[] ratings = {1};
        double[][] coords = {a};
        drawCircles(coords, ratings);

        try {
            addRangeSlider();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if(lastLocation != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)18.5));
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
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

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
                Log.i(LOG_TAG, "Location Updated");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
        mMap.setOnMapClickListener(this);

        Circle cirlce = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                .radius(50)
                .strokeColor(Color.parseColor("#FFA000"))
                .fillColor(Color.argb(30, 255, 222, 0))
                .strokeWidth(3));
    }

    public void postMessage(View v){
        Intent intent = new Intent(MapsActivity.this, PostActivity.class);
        Context context = getApplicationContext();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // Takes a list of coords and draws a circle at each point with a radius
    // relative to the rating of the post at that coord. Ratings passed as an equal-length list.
    // Randomly chooses a color for the circles from the color list.
    private void drawCircles(double[][] coords, int[] ratings){
        String[] colors = {"#81D4FA","#4FC3F7","#29B6F6","#03A9F4","#039BE5","#0288D1"};
        for(int i = 0; i< coords.length; i++){
            Circle cirlce = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(coords[i][0], coords[i][1]))
                    .radius(ratings[i])
                    .strokeColor(Color.parseColor(colors[0 + (int)(Math.random()*5)]))
                    .fillColor(Color.argb(30, 0, 162, 255))
                    .strokeWidth(3));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i("POSITION", latLng.toString());
    }

    private void addRangeSlider() throws ParseException {
        // create RangeSeekBar as Date range between 1950-12-01 and now
        Date minDate = new SimpleDateFormat("yyyy-MM-dd").parse("2015-05-06");
        Date maxDate = new Date();
        Context context = getApplicationContext();
        RangeSeekBar<Long> seekBar = new RangeSeekBar<Long>(minDate.getTime(), maxDate.getTime(), context);
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Long>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Long minValue, Long maxValue) {
                // handle changed range values
                Log.i("RANGESLIDER", "User selected new date range: MIN=" + new Date(minValue) + ", MAX=" + new Date(maxValue));
                TextView textView = (TextView) findViewById(R.id.dateTextView);
                textView.setText(""+new Date(minValue) + "\n"+ new Date(maxValue));
            }
        });

        // add RangeSeekBar to pre-defined layout

        ViewGroup layout = (ViewGroup) findViewById(R.id.sliderLayout);
        layout.addView(seekBar);
    }
}
