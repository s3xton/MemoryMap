package com.example.conor.project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity
    implements GoogleMap.OnMapClickListener{

    private static final String LOG_TAG = "MemoryMap";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final String SERVER_URL_PREFIX = "http://zach.ie/memorymap/query.php";
    private String[] colors = {"#81D4FA", "#4FC3F7", "#29B6F6", "#03A9F4", "#039BE5", "#0288D1"};
    private LocationManager locationManager;
    private Location lastLocation;
    private Criteria criteria;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static long lastchange;
    public PostInfo[] coordslist;
    public HashMap<String, PostInfo> circles = new HashMap<String, PostInfo>();
    public int[] ratings;
    private Circle viewableRadius;

    // Uploader.
    private ServerCall uploader;

    // To remember the posts we received.
    public static final String PREF_POSTS = "pref_posts";

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

        drawViewableRadius();
        // Add range slider to layout
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

        Intent intent = getIntent();
        if(intent.getStringExtra("result") != null && intent.getStringExtra("result").equals("success")){
            // Hide the splash screen and progress bar
            RelativeLayout splashScreen = (RelativeLayout) findViewById(R.id.splash_screen);
            splashScreen.setVisibility(View.INVISIBLE);
            // Show message successful toast
            Toast toast = Toast.makeText(getApplicationContext(), "Message successfully posted.", Toast.LENGTH_SHORT);
            toast.show();
            // Set map
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)18.5));
        }

        if(lastLocation != null && intent.getStringExtra("result") == null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)18.5));
        // Then start to request location updates, directing them to locationListener.
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
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

        mMap.setOnMapClickListener(this);

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (System.currentTimeMillis() - lastchange > 1000){
                    LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    getMessages(bounds.southwest.latitude, bounds.northeast.latitude,
                            bounds.southwest.longitude, bounds.northeast.longitude);
                    lastchange = System.currentTimeMillis();
                }
                if(mMap.getCameraPosition().zoom < 16.5){
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)16.5));
                }
            }
        });
    }

    public void updateMap(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        if(circles != null) {
            // Iterate over all our cached circles
            for (Map.Entry<String, PostInfo> entry : circles.entrySet()) {
                PostInfo p = entry.getValue();
                // Draw circle if it is in bounds and not already drawn
                if (bounds.contains(new LatLng(p.lat, p.lng))) {
                    if(p.circle == null) {
                        String color = colors[0];//colors[0 + (int) (Math.random() * 5)];
                        Circle circle = mMap.addCircle(new CircleOptions()
                                .center(new LatLng(p.lat, p.lng))
                                .radius(1)
                                .strokeColor(Color.parseColor(color))
                                .fillColor(Color.parseColor(color))
                                .strokeWidth(3));
                        p.circle = circle;
                    }
                } else { // Remove circles that are not shown
                    if(p.circle != null) {
                        p.circle.remove();
                        p.circle = null;
                    }
                }
            }
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
            drawViewableRadius();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    private void drawViewableRadius(){
        if(viewableRadius!=null)
            viewableRadius.remove();
        viewableRadius = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                .radius(50)
                .strokeColor(Color.parseColor("#FFA000"))
                .fillColor(Color.argb(30, 255, 222, 0))
                .strokeWidth(3));
    }

    public void getMessages(double latmin, double latmax, double lngmin, double lngmax){

        // Start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();
        myCallSpec.url = SERVER_URL_PREFIX;
        myCallSpec.context = this;

        // Let's add the parameters.
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("q", "get");
        m.put("type", "range");
        m.put("latmin", Double.toString(latmin));
        m.put("latmax", Double.toString(latmax));
        m.put("lngmin", Double.toString(lngmin));
        m.put("lngmax", Double.toString(lngmax));
        myCallSpec.setParams(m);

        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    public void postMessage(View v){
        Intent intent = new Intent(MapsActivity.this, PostActivity.class);
        Context context = getApplicationContext();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        double[] lastLocationArray = {lastLocation.getLatitude(),lastLocation.getLongitude()};
        Log.i("LLA",lastLocationArray[0]+"");
        intent.putExtra("lastLocationArray", lastLocationArray);
        context.startActivity(intent);
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
                textView.setText("" + new Date(minValue) + "\n" + new Date(maxValue));
            }
        });

        // add RangeSeekBar to pre-defined layout

        ViewGroup layout = (ViewGroup) findViewById(R.id.sliderLayout);
        layout.addView(seekBar);
    }

    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);

                Gson gson = new Gson();
                PostList ml = gson.fromJson(result, PostList.class);
                if(ml != null)
                    for (int i = 0; i < ml.posts.length; i++) {
                        circles.put(ml.posts[i].lat + "," + ml.posts[i].lng, ml.posts[i]);
                    }
                updateMap();

                // Hide the splash screen and progress bar
                RelativeLayout splashScreen = (RelativeLayout) findViewById(R.id.splash_screen);
                splashScreen.setVisibility(View.INVISIBLE);

                // Stores in the settings the last messages received.
                //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                //SharedPreferences.Editor editor = settings.edit();
                //editor.putString(PREF_POSTS, circles);
                //editor.commit();
            }
        }
    }
}



