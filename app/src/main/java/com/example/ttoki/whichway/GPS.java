package com.example.ttoki.whichway;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.GoogleMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class GPS extends Service implements GoogleApiClient.ConnectionCallbacks,
        LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GPS";

    //variables
    private LocationRequest locationRequest;
    private Location currentLocation;
    private String destination;
    private GoogleApiClient mGoogleApiClient;
    ResultReceiver resultReceiver;
    private String urlString;
    private JSONParser parser;
    private int step;
    private boolean routeDownloaded = false;
    private boolean aboutToTurn;
    private boolean justTurned = false;
    private boolean routeSent = false;
    private boolean isLastStep = false;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;
    LocationCallback locationCallback = new LocationCallback() {
    };

    @Override // Function called when service is started from  activity
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Creates a new requesr for current location from
        // Google Play Services
        createLocationRequest();

        // Create an instance of GoogleAPI client
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();

        // Set up ResultReceiver, to communicate with the UI thread in Main Activity
        // Destination and directions URL received
        resultReceiver = intent.getParcelableExtra("resultReceiver");

        if (null != intent.getExtras().getString("URL")) {

            // Extract URL for the directions request
            urlString = intent.getExtras().getString("URL");

            // Extract destination selected by user
            destination = intent.getExtras().getString("Destination");

            // This function retrieves the directions using the URL, from the
            // google direction API
            getDirections();
        }
        // Initialise step variable, this keeps track of
        // where the user is at
        step = 0;
        return super.onStartCommand(intent, flags, startId);
    }

    // Creates a new location request
    @SuppressLint("RestrictedApi")
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();

        // Set how frequently the GPS is updated
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(250);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

    }

    // Calls the inner class download directions
    private void getDirections() {
        try{
            new DownloadDirections().execute(
                    //Sends the URL to DownloadDirections
                    new URL(urlString));
        }catch(MalformedURLException m) {}

    }

    // Makes a web request for directions from the Google APU, thi is an asynchronous class
    // So it doesn't run on the main thread
    private class DownloadDirections extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... params) {
            BufferedReader buffer;
            InputStream stream = null;
            HttpsURLConnection connection = null;
            URL url = params[0];
            try {
                connection = (HttpsURLConnection) url.openConnection();
                connection.setReadTimeout(3000);
                connection.setConnectTimeout(3000);
                //set HTTP method to GET.
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                //Open communications link.
                connection.connect();
                int responseCode = connection.getResponseCode();
                if(responseCode != HttpsURLConnection.HTTP_OK)
                    throw new IOException("HTTP error code: " + responseCode);
                //Retrieve the response body as an InputStream.
                stream = connection.getInputStream();
                buffer = new BufferedReader(new InputStreamReader(stream));
                String nextLine;
                StringBuilder sb = new StringBuilder();
                while((nextLine = buffer.readLine()) != null) {
                    sb.append(nextLine + "\n");
                    Log.d(TAG, nextLine);
                }

                String directions = sb.toString();
                //Flag set so journey can start
                routeDownloaded = true;
                //Parser object initialized, will parse downloaded JSON directions
                parser = new JSONParser(directions);
                //newRoute = true;
            }
            catch(MalformedURLException m) {}
            catch(IOException e) {}
            finally {
                // Close Stream and disconnect HTTPS connection.
                try {
                    if (stream != null) stream.close();
                    if (connection != null) connection.disconnect();
                }
                catch(IOException e) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            stopSelf();
            super.onPostExecute(aLong);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Check that the app has permission for getting location
        checkPermission();
        // Assisgn currentLocation
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        startLocationUpdates();

    }

    protected void startLocationUpdates() {
        checkPermission();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);


    }
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        // Set globak variable "currentLocation" to be passed in location
        currentLocation = location;

        // If the route has been downloaded
        // List is ready
        if(routeDownloaded && parser.getListFull()){
            //If the route hasn't been sent to bluetooth
            if(!routeSent) {
                sendLatLngsTurnsToBluetoothService();
                routeSent = true;
            }
            checkUserIsBeginningTurn();
            checkUserIsMidTurn();
            checkUserHasMadeTurn();
        }
        sendBundle();
    }


    private void checkUserIsMidTurn() {
        //Set flags for user who is moving on to the next step,
        //increment counter for steps array
        if(getDistanceToTurn() < 20.0f && aboutToTurn) {
            aboutToTurn = false;
            justTurned = true;
            step++;
            isLastStep = checkIsLastStep();
        }
    }

    private boolean checkIsLastStep() {
        return step == parser.getStepsList().size() - 1;
    }

    private void checkUserHasMadeTurn() {
        //If the user has fully made the turn
        if(getDistanceToTurn() > 20 && justTurned){
            justTurned = false;
            turnOffTurnSignal();
        }
    }

    void checkUserIsBeginningTurn(){
        //If user crosses threshold approaching turn, light comes on to
        //indicate turn
        if(getDistanceToTurn() < 60.0f && !aboutToTurn) {
            Log.d(TAG, "inside about to turn");
            lightUpTurnSignal();
            aboutToTurn = true;
        }
    }

    //Turns on appropriate LED light on helmet
    private void lightUpTurnSignal() {
        Intent intent = new Intent("GPS to Bluetooth");

        //Get the direction to turn, taken from the next step in the array from the
        // current one as that is how the google direction JSON is formatted
        String direction;

        //Send the direction to the Bluetooth service
        if(!isLastStep)
            direction = parser.getStepsList().get(step + 1).getManeuver();
        else direction = "finish";
        intent.putExtra("direction", direction);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //Turns off LED light
    private void turnOffTurnSignal() {
        Intent intent = new Intent("GPS to Bluetooth");
        //Send "off" to Bluetooth service
        intent.putExtra("direction", "off");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //This list is made for when the board is used untethered
    // with its GPS module
    private ArrayList<String> createTurnsList() {
        ArrayList<String> turnsList = new ArrayList<>();
        //Iterates through stepslist, starts at index 1 because 0
        //never contains maneuver information
        for(int i = 1; i < parser.getStepsList().size(); i++){
            if(parser.getStepsList().get(i).getManeuver().equals("turn-left"))
                turnsList.add("L");
            else if(parser.getStepsList().get(i).getManeuver().equals("turn-right"))
                turnsList.add("R");
                //Some steps have no turns
            else turnsList.add("N");
        }
        //The last step is always straight, no turn
        turnsList.add("N");
        return turnsList;
    }

    //Send full array of lats, lng and turns to bluetooth service
    private void sendLatLngsTurnsToBluetoothService(){
        ArrayList<String> turns = createTurnsList();
        Intent intent = new Intent("GPS to Bluetooth");
        ArrayList<String> latiLongiTurnList = new ArrayList<>();

        for(int i = 0; i < parser.getStepsList().size(); i++){
            String lat = "lat" + parser.getStepsList().get(i).getEndLat();
            String lng = "lng" + parser.getStepsList().get(i).getEndLng();
            String man = turns.get(i);

            //Add each of the lat, lng and maneuver for each iteration.
            //Remove spaces needed because bluetooth module doesn't like whitespace
            latiLongiTurnList.add(removeSpaces(trimLatLng(lat)));
            latiLongiTurnList.add(removeSpaces(trimLatLng(lng)));
            latiLongiTurnList.add(removeSpaces(man));
        }
        //Marker for end of array
        latiLongiTurnList.add("end");

        //Send array to bluetooth service
        intent.putStringArrayListExtra("LatLng", latiLongiTurnList);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //Don't want latLng values greater than 13
    private String trimLatLng(String latLng) {
        if(latLng.length() > 13) return latLng.substring(0, 13);
        else return latLng;
    }

    private String removeSpaces(String latLngs){
        return latLngs.replaceAll("\\s+",""); }

    // Find the distance between the user and the next turn
    private float getDistanceToTurn() {
        //Get the Lat and lng of the next turn
        String endOfStepLat = parser.getStepsList().get(step).getEndLat();
        String endOfStepLng = parser.getStepsList().get(step).getEndLng();
        Location turn = new Location("turn");

        //Set lat and lng values of new Location object
        turn.setLatitude(Double.parseDouble(endOfStepLat));
        turn.setLongitude(Double.parseDouble(endOfStepLng));

        //Return distance between
        return currentLocation.distanceTo(turn);
    }


    private void sendBundle() {
        String routeLine = null;
        Bundle locationBundle = new Bundle();
        if(routeDownloaded && null != parser.getPolyline()){
            routeLine = parser.getPolyline();
        }

        locationBundle.putString("routeLine", routeLine);
        locationBundle.putString("latitude", Double.toString(currentLocation.getLatitude()));
        locationBundle.putString("longitude", Double.toString(currentLocation.getLongitude()));
        resultReceiver.send(0, locationBundle);
    }


    @Override
    public void onConnectionSuspended(int result) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION)
                != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                COARSE_LOCATION) != PERMISSION_GRANTED) {
            return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}