package com.example.ttoki.whichway;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "MapsActivity";

    //variables
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LatLng presentLatitudeLongitude = null;
    private LatLng destinationLatitudeLongitude = null;
    private boolean initialRefresh = true;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean bluetoothConnected;
    private boolean bluetoothDisconnected;
    private boolean bluetoothFailedToConnect;
    private boolean bluetoothScanning;
    private MenuItem bluetoothItem;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;
    private boolean startPicker = true;
    private Polyline routeLine = null;
    private Double destinationAddressLongitude;
    private Double destinationAddressLatitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.maps_toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    //when the program starts, ask the user for permission to get their location
    //if the user clicks true, then start the initialisation
    protected void onStart() {
        super.onStart();
        if (this.checkSelfPermission(COARSE_LOCATION) != PERMISSION_GRANTED) {
            dialogBuilder("Location Access Request", "Location Access Granted", true, false);
        } else initialization();
    }


    //initialise the google places authentication
    private void initialization() {

        placesInitializer();
        apiClientInitializer();
        gpsInitializer();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    // When the program stops, disconnect from using google's credential key
    protected void onStop() {
        super.onStop();
        if (null != mGoogleApiClient) mGoogleApiClient.disconnect();
    }

    protected void onDestroy() {
        super.onDestroy();
        this.stopService(new Intent(this, GPS.class));
    }


    // Dialog builder to interact with the verification system
    // once okay relay out the messages for the okay string
    // if not okay, cancel and give reason why
    // if verification is successful, let the user know as well as granting them location
    void dialogBuilder(String title, String message, boolean v, boolean s) {

        final boolean verification = v;
        final boolean scanning = s;
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message);
        if (verification) alertDialogBuilder.setPositiveButton(android.R.string.ok, null);
        else alertDialogBuilder.setPositiveButton(android.R.string.cancel, null);
        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (verification)
                    requestPermissions(new String[]{COARSE_LOCATION},
                            PERMISSION_REQUEST_COARSE_LOCATION);
                if(scanning) {
                    stopService(new Intent(getApplicationContext(), BluetoothLeService.class));
                    Toast.makeText(getApplicationContext(), "Bluetooth service has stopped",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialogBuilder.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // These are the buttons that will be displayed on the screen
    // a bluetooth button, bicycle logo and a refresh logo
    // When any of these buttons are clicked
    // These are the functionalities they are meant to do
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bluetooth_search) {
            bluetoothItem = item;
            BluetoothServiceStartup();
        }
        else if (id == R.id.bicycleLogo)
            gpsInitializer();
        else if (id == R.id.refreshLogo)
            mapRefresh();
        return super.onOptionsItemSelected(item);
    }

    // If you click for for the map to refresh
    // the route should disappear
    private void mapRefresh() {
        if (null != routeLine) {
            routeLine.remove();
        }
        mMap.clear();
        destinationLatitudeLongitude = null;
        startPicker = true;
        routeLine = null;
    }


    //the result of the location is 1 for successful, 0 for unsuccessful
    //if by any chance you are given authorisation, initialise
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] verified) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (verified[0] == PERMISSION_GRANTED) initialization();
            }
        }
    }

    //this connects the app to the google api
    //allows the user to use the location services provided
    private void apiClientInitializer() {

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    private void gpsInitializer() {
        //The receiver for communicating from the service back to MapsActivity
        DataRetrieval dataRetrieval = new DataRetrieval(null, this);
        Intent gpsInit = new Intent(this, GPS.class);

        //Send the receiver with the Intent
        gpsInit.putExtra("resultReceiver", dataRetrieval);

        //If the user has already selected a destination
        if (destinationLatitudeLongitude != null) {
            String origin = presentLatitudeLongitude.latitude + "," + presentLatitudeLongitude.longitude;
            String destination = Double.toString(destinationAddressLatitude) + "," + Double.toString(destinationAddressLongitude);

            //Makes a URL string from the current location and destination
            String url = new DirectionsURL().makeDirectionsURL(origin, destination);
            gpsInit.putExtra("Origin", origin);
            gpsInit.putExtra("URL", url);
            gpsInit.putExtra("Destination", destination);
        }
        startService(gpsInit);
    }


    //initialise the default map given
    //use credential key given by google api to acquire google locations and google places
    //autocomplete fragment to allow users to select location before it is fully typed in
    //The place selected should display the id and the name of the place
    private void placesInitializer() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyA_16XMwKNjCHR9YMhK2-LXnwU6kw0-RNs");
        }
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                //TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                //this gives autocomplete its functionality to be able to pinpoint the location on the map that is selected by the user
                //when the user clicks on the location suggestion, this method will bring the user to that location and place a marker on it
                geoLocate(place.getName());
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }


    //collects the destination in latitude and longitude format and uses a method to move the camera to that location
    //gets a list containing all the information of a location
    //selects the search string that contains the lat and lng
    //collects this data and processes it
    private void geoLocate(String searchS) {
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = searchS;
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            destinationAddressLongitude = address.getLongitude();
            destinationAddressLatitude = address.getLatitude();
            moveCamera(new LatLng( destinationAddressLatitude,  destinationAddressLongitude ), 17, address.getAddressLine(0));


        }
    }


    //collects the latitude and longitude, is able to zoom and also displays the title of the location of where you are  or where the selected location is
    //places a marker on the location
    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }

    }


    //places the marker fot both the user when the map is ready and places the marker for the destination
    private void placeMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
        if(startPicker) {
            startPicker = false;
            destinationLatitudeLongitude = latLng;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onConnected(@Nullable Bundle connectionHint) { }

    @Override
    public void onConnectionSuspended(int i) { }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occured and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently.
    }

    void gpsRefresh(String la, String lo, String pol, String dir, String dis) {

        double latitude = Double.parseDouble(la);
        double longitude = Double.parseDouble(lo);
        presentLatitudeLongitude = new LatLng(latitude, longitude);

        if (initialRefresh) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(presentLatitudeLongitude, 17));
            initialRefresh = false;
        }

        if(null != pol) {
            routelineRefresh(pol);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION)
                != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                COARSE_LOCATION) != PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                placeMarker(latLng);
            }
        });
    }

    void routelineRefresh(String p) {
        List<LatLng> routeLineList = PolyUtil.decode(p);
        routeLine = mMap.addPolyline(new PolylineOptions()
                .addAll(routeLineList)
                .width(10)
                .color(ContextCompat.getColor(getApplicationContext(), R.color.routeColour))
                .geodesic(true));
    }


    //before the location can be changed, request for permission,if granted, proceed
    //when the location is changing, acquire the new position of where the user is
    //keep displaying the LatLng location to show their current position and keep updating
    //move the camera to that specific location whether they stand still or move
    @Override
    public void onLocationChanged(Location location) {


        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION)
                != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                COARSE_LOCATION) != PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                        }
                    }
                });
    }

    private void BluetoothServiceStartup() {
        BluetoothReceiver BluetoothReceiver = new BluetoothReceiver(null, this);
        Intent BluetoothService = new Intent(this, BluetoothLeService.class);
        BluetoothService.putExtra("resultReceiver", BluetoothReceiver);
        startService(BluetoothService);
    }

    public void onReceiveBluetoothUpdate(Bundle resultData) {
        if (null != resultData.getString("Connected")) {
            connectionStatus(true, false, false, false);
            updateUiBluetooth("Bluetooth Connected", null);
        } else if (null != resultData.getString("Disconnected")) {
            connectionStatus(false, true, false, false);
            //updateUiBluetooth("Bluetooth Disconnected", null);
        } else if (null != resultData.getString("Failed to Connect")) {
            connectionStatus(false, true, true, true);
            //updateUiBluetooth("Failed to Connect Bluetooth", null);
        } else if (null != resultData.getString("No device")) {
            connectionStatus(false, true, true, true);
            //updateUiBluetooth("No Bluetooth Device Present", null);
        } else if (null != resultData.getString("Scanning")) {
            connectionStatus(false, true, false, true);
            //updateUiBluetooth("Scanning for Bluetooth Device...", null);
        }
    }

    void updateUiBluetooth(String s, String p) {
        final String message = s;
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(bluetoothConnected)
                    bluetoothItem.setIcon(R.drawable.ic_bluetooth_black_24dp);
                else
                    bluetoothItem.setIcon(R.mipmap.ic_bluetooth_white_24dp);
                if(bluetoothScanning)
                    //dialogBuilder("Scanning", "Scanning for device...", false, true);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectionStatus(boolean connected, boolean disconnected, boolean failed, boolean scanning) {
        bluetoothConnected = connected;
        bluetoothDisconnected = disconnected;
        bluetoothFailedToConnect = failed;
        bluetoothScanning = scanning;
    }


}