package uk.ac.cam.km662.arm_hackathon_2016;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class StartDirections extends AppCompatActivity implements OnConnectionFailedListener {

    private static final String TAG = StartDirections.class.getSimpleName();
    private Button startNavigation;

    private PlaceAutocompleteFragment inputStartDestination;
    private PlaceAutocompleteFragment inputEndDestination;

    private TextView editLocation;
    private ProgressBar progressBar;

    private GoogleApiClient mGoogleApiClient;
    private String API_KEY =  "AIzaSyD7iDVVCQi719H5DK93NKWqZkJt1E3YRNs";

    private Boolean flag = false;
    private Boolean firstLocation = true;

    private LocationListener locationListener = null;
    private LocationManager locationManager = null;
    private LatLng endLocation;
    private LatLng startLocation;

    private String txtRoute = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_directions);

        inputStartDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.directions_from);
        inputEndDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.directions_to);

        startNavigation = (Button) findViewById(R.id.startNavigation);
        editLocation = (TextView) findViewById(R.id.locationText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        progressBar.setVisibility(View.INVISIBLE);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mGoogleApiClient.connect();


        inputStartDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                //Get info about the selected place.
                startLocation = place.getLatLng();
                editLocation.setText("dest_latitude: " + startLocation.latitude + " dest_longitude: " + startLocation.longitude);
                Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        inputEndDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                //Get info about the selected destination.
                endLocation = place.getLatLng();
                editLocation.setText("dest_latitude: " + endLocation.latitude + " dest_longitude: " + endLocation.longitude);
                Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });


        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBvcstUhXLM25Ob5_fN4UaJMLAuVbUNOyw");
                context .setQueryRateLimit(3)
                        .setConnectTimeout(1, TimeUnit.SECONDS)
                        .setReadTimeout(1, TimeUnit.SECONDS)
                        .setWriteTimeout(1, TimeUnit.SECONDS);


                DirectionsResult results = null;
                try {
                    results = DirectionsApi.newRequest(context)
                            .mode(TravelMode.BICYCLING)
                            .avoid(DirectionsApi.RouteRestriction.HIGHWAYS, DirectionsApi.RouteRestriction.TOLLS, DirectionsApi.RouteRestriction.FERRIES)
                            .units(Unit.METRIC)
                            .region("uk")
                            .origin(startLocation.latitude + "," + startLocation.longitude)
                            .destination(endLocation.latitude + "," + endLocation.longitude).await();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String oneDirection = null;
                String duration = "";
                String fullRoute = "";
                if (results.routes.length != 0){
                    DirectionsRoute firstRoute = results.routes[0];
                    for(int i = 0; i < firstRoute.legs.length; i++){
                        for (int j = 0; j < firstRoute.legs[i].steps.length; j++){

                            oneDirection = firstRoute.legs[i].steps[j].htmlInstructions;
                            //get duration in ms
                            duration = String.valueOf(firstRoute.legs[i].steps[j].duration.inSeconds * 1000L);
                            fullRoute += "\n" + firstRoute.legs[i].steps[j].htmlInstructions + " duration " + firstRoute.legs[i].steps[j].duration.inSeconds;
                            /**
                             * Parse DirectionsStep String
                             *
                             * if contains right --> turn right
                             * if contains left --> turn left
                             * if roundabout
                             *      --> 1st exit --> turn left
                             *      --> 2nd exit --> go straight
                             *      --> 3rd exit + --> turn right
                             * else go straight and output time to go straight RAND(0,10)
                             */

                            if (oneDirection.contains("left")
                                    || (oneDirection.contains("roundabout") && oneDirection.contains("1st"))){

                                txtRoute += "1 " + duration + "\n";

                            } else if (oneDirection.contains("roundabout") && (oneDirection.contains("2nd") || oneDirection.contains("through"))){

                                txtRoute += "2 "+ duration +"\n";

                            } else if (oneDirection.contains("right")
                                    || oneDirection.contains("roundabout")){

                                txtRoute +="3 " + duration + "\n";

                            } else {

                                txtRoute += "2 "+ duration + "\n";

                            }

                        }
                    }
                } else {
                    txtRoute = "0"; //no route found
                }

                //editLocation.setText(txtRoute + fullRoute);










//                flag = displayGpsStatus();
//                if (flag) {
//                    editLocation.setText("Move device to see changes.");
//                    progressBar.setVisibility(View.VISIBLE);
//
//                    locationListener = new MyLocationListener();
//                    try {
//                        locationManager.requestLocationUpdates(LocationManager
//                                .GPS_PROVIDER, 5000, 10, locationListener);
//                    } catch (SecurityException e) {
//                        System.out.print("No permission to access location.");
//                    }
//
//                } else {
//                    alertbox("Gps Status!!", "Your GPS is: OFF");
//                }
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Could not connect to Google APIs.
    }

    protected void alertbox(String title, String mymessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your Device's GPS is Disable")
                .setCancelable(false)
                .setTitle("** Gps Status **")
                .setPositiveButton("Gps On",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_SECURITY_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

//    private class MyLocationListener implements LocationListener {
//        @Override
//        public void onLocationChanged(Location loc) {
//
//            if (firstLocation){ //when we get first location, set to start location and get route
//                //startLocation = loc;
//                firstLocation = false;
//
//
//            }
//
//            editLocation.setText("");
//            progressBar.setVisibility(View.INVISIBLE);
//            Toast.makeText(getBaseContext(),"Location changed : Lat: " +
//                            loc.getLatitude()+ " Lng: " + loc.getLongitude(),
//                    Toast.LENGTH_SHORT).show();
//            String longitude = "Longitude: " +loc.getLongitude();
//            Log.v(TAG, longitude);
//            String latitude = "Latitude: " +loc.getLatitude();
//            Log.v(TAG, latitude);
//
//    /*----------to get City-Name from coordinates ------------- */
//            String cityName=null;
//            Geocoder gcd = new Geocoder(getBaseContext(),
//                    Locale.getDefault());
//            List<Address>  addresses;
//            try {
//                addresses = gcd.getFromLocation(loc.getLatitude(), loc
//                        .getLongitude(), 1);
//                if (addresses.size() > 0)
//                    System.out.println(addresses.get(0).getLocality());
//                cityName=addresses.get(0).getLocality();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            String s = longitude+"\n"+latitude +
//                    "\n\nMy Currrent City is: "+cityName;
//            editLocation.setText(s);
//        }
//
//        public Location getParsedLocation(LatLng latLng){
//            Location mapLocParsed = new Location("LatLng");
//            mapLocParsed.setLongitude(latLng.longitude);
//            mapLocParsed.setLatitude(latLng.latitude);
//            mapLocParsed.setAltitude(0);
//            return mapLocParsed;
//        }
//
//        public boolean isWithin(Location A, Location B, double range){
//            return (A.distanceTo(B) < range);
//        }
//
//
//        @Override
//        public void onProviderDisabled(String provider) {
//            // TODO Auto-generated method stub
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//            // TODO Auto-generated method stub
//        }
//
//        @Override
//        public void onStatusChanged(String provider,
//                                    int status, Bundle extras) {
//            // TODO Auto-generated method stub
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_directions, menu);
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
}
