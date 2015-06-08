package com.example.conor.project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class MapsActivity extends FragmentActivity
    implements GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener{

    private static final String LOG_TAG = "MemoryMap";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final String SERVER_URL_PREFIX = "http://zach.ie/memorymap/";
    private String[] colors = {"#81D4FA", "#4FC3F7", "#CD88AF", "#661141"};
    private LocationManager locationManager;
    private Location lastLocation;
    private Criteria criteria;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static long lastchange;
    public HashMap<String, PostInfo> circles = new HashMap<String, PostInfo>();
    public static HashMap<Marker, PostInfo> markers = new HashMap<Marker, PostInfo>();
    public int[] ratings;
    private Circle viewableRadius;
    private Set<String> readMarkers;
    private static final int VIEW_RADIUS = 50;

    private PostInfo image_retrieve_url;

    // Uploader.
    private ServerCall uploader;

    // To remember the posts we received.
    public static final String PREF_POSTS = "readmarkers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        lastLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

        // Set up map variables
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        drawViewableRadius();
        lastchange = System.currentTimeMillis();

        // Animate camera to current location
        if(lastLocation != null)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float)18.5));

        // Add range slider to layout
        try {
            addRangeSlider();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Get read markers from cache
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        readMarkers = settings.getStringSet(PREF_POSTS, new HashSet<String>());
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
            intent.removeExtra("result");
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
        mMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));

        mMap.setMyLocationEnabled(true);

        mMap.setOnMapClickListener(this);

        mMap.setOnMarkerClickListener(this);

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (System.currentTimeMillis() - lastchange > 1000) {
                    LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    getMessages(bounds.southwest.latitude, bounds.northeast.latitude,
                            bounds.southwest.longitude, bounds.northeast.longitude);
                    lastchange = System.currentTimeMillis();
                }
                if (mMap.getCameraPosition().zoom < 16.5) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), (float) 16.5));
                }
            }
        });
    }


    public void updateMap(){
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        int radius = 3;
        if(circles != null) {
            // Iterate over all our cached circles
            for (Map.Entry<String, PostInfo> entry : circles.entrySet()) {
                PostInfo p = entry.getValue();
                // Draw circle if it is in bounds and not already drawn
                if (bounds.contains(new LatLng(p.lat, p.lng))) {
                    if (p.circle == null) {
                        String color = colors[0];//colors[0 + (int) (Math.random() * 5)];
                        Circle circle = mMap.addCircle(new CircleOptions()
                                .center(new LatLng(p.lat, p.lng))
                                .radius(radius)
                                .strokeColor(Color.parseColor("#AAAAAA"))
                                .fillColor(Color.parseColor("#CCCCCC"))
                                .strokeWidth(3));
                        p.circle = circle;

                        // Check and see if the circle is with the large cirlce radius and draw
                        // markers if they are
                        float[] d = new float[2];
                        Location.distanceBetween(p.lat, p.lng, lastLocation.getLatitude(), lastLocation.getLongitude(), d);
                        if(d[0] < VIEW_RADIUS || readMarkers.contains(p.lat+","+p.lng)){
                            //place marker
                            Marker circleMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(p.lat, p.lng))
                                    .title(getTimeAgo(p.time))
                                    .snippet(p.data)
                                    .infoWindowAnchor((float) 0.5, (float)
                                            1.0));
                            // Set to blue if within radius
                            if(d[0] < VIEW_RADIUS){
                                p.circle.setStrokeColor(Color.parseColor(colors[1]));
                                p.circle.setFillColor(Color.parseColor(colors[0]));
                            }
                            // Set to purple if marker was opened
                            if(readMarkers.contains(p.lat+","+p.lng)){
                                p.circle.setStrokeColor(Color.parseColor(colors[3]));
                                p.circle.setFillColor(Color.parseColor(colors[2]));
                            }
                            p.circleMarker = circleMarker;
                            p.circleMarker.setAlpha(0);
                            markers.put(circleMarker, p);
                        }
                    } else { // Remove circles that are not shown
                        if (p.circle != null) {
                            p.circle.remove();
                            p.circle = null;
                        }
                        if (p.circleMarker != null) {
                            p.circleMarker.remove();
                            p.circleMarker = null;
                        }
                    }
                }
            }
        }
    }

    private String getTimeAgo(String input){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        try {
            date = format.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return (String) DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), 0);
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
                .radius(VIEW_RADIUS)
                .strokeColor(Color.parseColor("#FFA000"))
                .fillColor(Color.argb(30, 255, 222, 0))
                .strokeWidth(3));
    }

    public void getMessages(double latmin, double latmax, double lngmin, double lngmax){

        // Start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();
        myCallSpec.url = SERVER_URL_PREFIX + "query.php";
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

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        PostInfo p = markers.get(marker);
        String key = marker.getPosition().toString();
        if(p.image != null) {
            image_retrieve_url = p;
            new ImageDownloader().execute();
        }
        markAsRead(marker, p);
        return true;
    }

    public void markAsRead(Marker marker, PostInfo p){
        // Change color to purple
        p.circle.setStrokeColor(Color.parseColor(colors[3]));
        p.circle.setFillColor(Color.parseColor(colors[2]));
        // Add to read markers cache and save
        readMarkers.add(p.lat+","+p.lng);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(PREF_POSTS, readMarkers);
        editor.commit();
    }

    public void setIconForMarker(Bitmap bmp){
        image_retrieve_url.bitmap = bmp;
        image_retrieve_url.circleMarker.hideInfoWindow();
        image_retrieve_url.circleMarker.showInfoWindow();
    }

    private class ImageDownloader extends AsyncTask {
        private Bitmap bmp;

        @Override
        protected void onPostExecute(Object result) {
            if (bmp != null) {
                setIconForMarker(bmp);
            }
        }

        @Override
        protected Bitmap doInBackground(Object[] param) {
            try {
                InputStream in = new URL(SERVER_URL_PREFIX + "images/" + image_retrieve_url.image).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                // log error
            }
            return null;
        }

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

            }
        }
    }
}



