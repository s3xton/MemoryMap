package com.example.conor.project;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;


public class PostActivity extends ActionBarActivity {
    private static final String LOG_TAG = "MemoryMap";
    private static final String SERVER_URL_PREFIX = "http://zach.ie/memorymap/query.php";
    private static int RESULT_LOAD_IMG = 1;

    private GoogleMap mMap;
    private double[] lastLocationArray;
    private LocationManager locationManager;
    private Criteria criteria;
    private String imgDecodableString;
    private Bitmap bitmap;
    private String filename;
    private String encodedString;

    // Uploader.
    private ServerCall uploader;

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

    public void clickPhoto(View v){
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    // Function stub for when the post button is pressed
    public void clickPost(View v){
        if(imgDecodableString != null) {
            Toast toast = Toast.makeText(getApplicationContext(), "Encoding image...", Toast.LENGTH_SHORT);
            toast.show();
            encodeImagetoString();
        } else {
            finishUpload();
        }
    }

    public void finishUpload(){
        if(encodedString != null) {
            Toast toast = Toast.makeText(getApplicationContext(), "Uploading image...", Toast.LENGTH_SHORT);
            toast.show();
        }
        EditText editText = (EditText) findViewById(R.id.editText);
        String textToPost = editText.getText().toString();


        // Start the call.
        PostMessageSpec myCallSpec = new PostMessageSpec();
        myCallSpec.url = SERVER_URL_PREFIX;
        myCallSpec.context = this;

        // Let's add the parameters.
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("q", "set");
        m.put("data",textToPost);
        if(encodedString != null) {
            m.put("type", "image");
            m.put("image", encodedString);
            m.put("filename", filename);
        } else {
            m.put("type", "text");
        }
        m.put("lat", Double.toString(lastLocationArray[0]));
        m.put("lng", Double.toString(lastLocationArray[1]));
        myCallSpec.setParams(m);

        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }

    public void finishPost(Context context, PostList res){
        if(res.result.equals("success")){
            Intent intent = new Intent(PostActivity.this, MapsActivity.class);
            intent.putExtra("result", res.result);
            context.startActivity(intent);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Error:" + res.description, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };


                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();

                // Get filename
                String fileNameSegments[] = imgDecodableString.split("/");
                filename = fileNameSegments[fileNameSegments.length - 1];

                // Set background of button to bitmap
                ImageView iv = (ImageView) findViewById(R.id.photoButton);
                bitmap = BitmapFactory.decodeFile(imgDecodableString);
                iv.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "No image selected.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }


    // AsyncTask - To convert Image to String
    public void encodeImagetoString() {
        new AsyncTask<Void, Void, String>() {

            protected void onPreExecute() {

            };

            @Override
            protected String doInBackground(Void... params) {
                BitmapFactory.Options options = null;
                options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // Must compress the Image to reduce image size to make upload easy
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                byte[] byte_arr = stream.toByteArray();
                // Encode Image to String
                encodedString = Base64.encodeToString(byte_arr, 0);
                return "";
            }

            @Override
            protected void onPostExecute(String msg) {
                finishUpload();
            }
        }.execute(null, null, null);
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

                finishPost(context, ml);

                // Stores in the settings the last messages received.
                //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                //SharedPreferences.Editor editor = settings.edit();
                //editor.putString(PREF_POSTS, circles);
                //editor.commit();
            }
        }
    }
}
