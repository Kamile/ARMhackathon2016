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
import android.net.Uri;
import android.os.Handler;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

    private String fileName = "outputRoute.cpp";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_directions);

        inputStartDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.directions_from);
        inputEndDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.directions_to);

        inputStartDestination.setHint("Start");

        inputEndDestination.setHint("Destination");

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
                String txtRoute = "";
                if (results.routes.length != 0){
                    DirectionsRoute firstRoute = results.routes[0];
                    for(int i = 0; i < firstRoute.legs.length; i++){
                        for (int j = 0; j < firstRoute.legs[i].steps.length; j++){

                            oneDirection = firstRoute.legs[i].steps[j].htmlInstructions;
                            //get duration in ms
                            duration = String.valueOf(firstRoute.legs[i].steps[j].duration.inSeconds * 1000L);
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

                                txtRoute += "1 " + duration + ".";

                            } else if (oneDirection.contains("roundabout") && (oneDirection.contains("2nd") || oneDirection.contains("through"))){

                                txtRoute += "2 "+ duration +".";

                            } else if (oneDirection.contains("right")
                                    || oneDirection.contains("roundabout")){

                                txtRoute +="3 " + duration + ".";

                            } else {
                                txtRoute += "2 "+ duration + ".";

                            }

                        }
                    }
                } else {
                    txtRoute = "0"; //no route found
                }

                createCPP(txtRoute);

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



    public static void lightup(PrintWriter writer, int pinVal, int duration, int nextpinVal) {
        //turn off the rest
        int holdDirtime = 0;
        int timeFactor = 10;
        writer.println("	uBit.io.P0.setDigitalValue(0);");
        writer.println("	uBit.io.P1.setDigitalValue(1);");
        writer.println("	uBit.io.P2.setDigitalValue(0);");

        //set how long you will keep the light in one direction
        //if (duration > 3000) {
        //duration-=3000;
        //holdDirtime = 3000;
        //} else {
        //holdDirtime = duration;
        //duration =0;
        //}
        //light up for 3000(or less) seconds
        //writer.println("	uBit.io.P"+pinVal+".setDigitalValue(1);"); //light up for 3s or less
        //writer.println("	uBit.sleep("+holdDirtime+");");
        //writer.println("	uBit.io.P"+pinVal+".setDigitalValue(0);"); //off dir, change back to straight
        //writer.println("	uBit.io.P1.setDigitalValue(1);");

        if (nextpinVal != 1) {
            if (duration >= 6000) {
                //continue straight until last 6 secs then point next directn blinking
                writer.println("	uBit.sleep(" + (duration - 6000)/timeFactor + "); ");
                writer.println("	uBit.io.P1.setDigitalValue(0);");
                writer.println("	for (int i=0; i<3; i++) {");
                writer.println("	uBit.io.P" + nextpinVal + ".setDigitalValue(1);");
                writer.println("	uBit.sleep(50); ");
                writer.println("	uBit.io.P" + nextpinVal + ".setDigitalValue(0);");
                writer.println("	uBit.sleep(50);} ");
                writer.println("	uBit.io.P" + nextpinVal + ".setDigitalValue(1);");
                writer.println("	uBit.sleep("+(3000/timeFactor)+"); ");
                writer.println("	uBit.io.P1.setDigitalValue(1);");
            } else {
                writer.println("	uBit.io.P1.setDigitalValue(0);");
                writer.println("	for (int i=0; i<"+(duration-3000)/timeFactor+"; i+=100) {");
                writer.println("	uBit.io.P" + nextpinVal + ".setDigitalValue(1);");
                writer.println("	uBit.sleep(50); ");
                writer.println("	uBit.io.P" + nextpinVal + ".setDigitalValue(0);");
                writer.println("	uBit.sleep(50);} ");
                writer.println("	uBit.io.P1.setDigitalValue(1);");
            }
        } else {
            writer.println("	uBit.sleep(" + duration/timeFactor + "); ");
        }


    }


    public void createCPP(String val) {
        File file = new File(this.getExternalFilesDir(null), "main.cpp");
        System.out.println(this.getExternalFilesDir(null).getAbsolutePath());
        PrintWriter writer = null;

        int timeFactor = 1;
        try {

            String[] singleCommands = null;
            //break up into single commands of direction and duration
            singleCommands = val.split("\\.");

            //break up into duration and line commands
            String[][] dirNtime = new String[singleCommands.length][2];
            for (int i=0; i<singleCommands.length; i++) {
                dirNtime[i] = singleCommands[i].split(" ");
                //System.out.println(dirNtime[i][0]);
                //System.out.println(dirNtime[i][1]);

            }

            writer = new PrintWriter(file);
            writer.println("#include \"MicroBit.h\"");
            writer.println("#include <fstream>");
            writer.println("#include <string.h>");
            writer.println();
            writer.println("MicroBit uBit;");
            writer.println("int main() {");
            writer.println("	uBit.init();");

            int duration, pinVal=1,nextpinVal;

            for (int i=0; i<dirNtime.length; i++) {
                duration = Integer.parseInt(dirNtime[i][1]);
                duration /= timeFactor;
                nextpinVal=1;
                if (Integer.parseInt(dirNtime[i][0]) == 1) {
                    //left
                    pinVal = 0;
                    //get next pin value if there is one
                    if ((i+1)<dirNtime.length) {
                        if (Integer.parseInt(dirNtime[i+1][0]) == 1) nextpinVal = 0;
                        if (Integer.parseInt(dirNtime[i+1][0]) == 3) nextpinVal = 2;
                    }
                    //only light for short while
                    lightup(writer,pinVal,duration, nextpinVal);

                } else if (Integer.parseInt(dirNtime[i][0]) == 2) {
                    //straight
                    pinVal = 1;
                    if ((i+1)<dirNtime.length) {
                        if (Integer.parseInt(dirNtime[i+1][0]) == 1) nextpinVal = 0;
                        if (Integer.parseInt(dirNtime[i+1][0]) == 3) nextpinVal = 2;
                    }
                    lightup(writer,pinVal,duration, nextpinVal);
                } else if (Integer.parseInt(dirNtime[i][0]) == 3) {
                    //right
                    pinVal = 2;
                    if ((i+1)<dirNtime.length) {
                        if (Integer.parseInt(dirNtime[i+1][0]) == 1) nextpinVal = 0;
                        if (Integer.parseInt(dirNtime[i+1][0]) == 3) nextpinVal = 2;
                    }
                    lightup(writer,pinVal,duration, nextpinVal);
                }
            }
            //writer.println("	uBit.io.P0.setDigitalValue(1);");
            //writer.println("	uBit.io.P1.setDigitalValue(1);");
            //writer.println("	uBit.io.P2.setDigitalValue(1);");
            writer.println("	release_fiber(); }");


        } catch (FileNotFoundException e) {
            System.err.println("File wasn't found.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO failed.");
            e.printStackTrace();
        }

        writer.close();
        System.out.println("Print successful");
        sendRouteEmail();
    }

    private void sendRouteEmail(){
        String fileLocation = "/storage/emulated/0/Android/data/uk.ac.cam.km662.arm_hackathon_2016/files/main.cpp";
        File file = new File(fileLocation);
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dsjw2@cam.ac.uk"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "route");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

        try{
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
               /* Create an Intent that will send e-mail. */
                    startActivity(Intent.createChooser(intent, "Send route"));
                }
            }, 3000);

            System.out.println("Email Sent");
        } catch (android.content.ActivityNotFoundException e){
            Toast.makeText(StartDirections.this, "There are no e-mail clients installed.", Toast.LENGTH_SHORT).show();
        }

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
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Could not connect to Google APIs.
    }

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
