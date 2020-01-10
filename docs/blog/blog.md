# Blog: Which Way


**TOBA TOKI**

## Blog Entry 1. Title: Looking for what hardware to buy. Date: 27/10/18


The first step I am taking towards this project is to first of all buy the parts
that I need to make this project. This part is quite difficult to do as I have
to understand what particular hardware it is that I need. There is an company
that make Arduino wearables that are customizable. These wearables are able to
be sewn on any piece of clothing. I decided that for my project, I would first
of all need the board that will accept the code and send to the other hardware.
Since I am using my phone to try and have contact with this board, I would need
to purchase a Bluetooth device that would be able to communicate with my phone
as well as the board. The name of the company is called Adafruit Flora. They
also sell led lights called neopixels that can change colour depending on what
you code it to be. I will also buy this. They also sell GPS tracking device
which is good for having a fixed location of where the person will be.

## Blog Entry 2. Title: Recieved items. Date: 1/11/18

Today I recieved my order of the hardware that I purchased. Everything works as it is supposed to

## Blog Entry 3. Title: Starting with Android Studio. Date: 3/11/18

My aim for this week is to familiarise myself with android studio in regards to dealing with location retrieval.
When you want to create an app on android studio, it gives you a number of templates to use. I chose the "Map Activity" template.
This generates a java class called "Maps Activity" with pre-written code of zoning into a fixed location. It also creates xml files that can be used to design what I want.
I tested the pre-written code on my android phone. 

## Including Code

Java:
```java 
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
```

## Blog Entry 4. Title: Creating a Google Maps API KEY. Date: 4/11/18

My plan for this week is to be able to get a Google Maps API KEY. The reason for this is to create an interaction between my android application and google maps api.
This communocation will allow me to be able receive helpul data about places and locations. It will be able to cause maps to appear for my app.
It will also be able to return data about a latitude and longitude location as well as returning data about an address.
The way I will do this is by creating a project on google console.
I would have to retrieve a authentication key that creates a specific credential key. 
Using this credential key, i will be able to enable google maps API. 
I then use this credential key in my android application and this should establish communocation between my app and google maps.

## Blog Entry 5. Title: Using Credential Key to enable Google Places API. Date: 7/11/18

After being able to enable google maps api and being able to access my location and move around the map, it was time for me to enable google places api. 
Google places is a web listing that includes photos, reviews and a map and other information about a business. It places a pin on a selected location and from this waypoint pin, you are able to retrieve the information of where the waypoint pin is on.
This is important for my application because it can display the name and other attributes that I decide to display. 

## Blog Entry 6. Title: Working with Flora Board. Date: 15/11/18

My plan for the next 2 weeks is to familiarise myself with every hardware that will be communicating with the Flora board. 
I plan to know what the pins on the board and other hardware is and which pins needs to be used to get the right data across.
I can find these documentations on the Adafruit website. I will have to read a lot of what each hardware does.
First I'm going to try communicate with the neopixels. If it works as well as I want then I will check the GPS module and the Bluetooth module.


## Blog Entry 7. Title: Establishing a connection with android smartphone and bluetooth module using Adafruit App. Date 25/11/18

Establishing a connection with my android phone with the flora board. What I would like to do at before the end of this week is to send information to and from my phone to the board via the bluetooth module.
I want to do this with an app created by adafruit called the fluefruit app. I would like to send information and receive a message back from the flora that the message I sent was received.
Doing this will get rid of any stops that could happen in the future regarding data passing between my phone and the flora board.
Once this is done, I will this connection to see if I can interact with the neopixels using the bluetooth connection.

## Blog Entry 8. Title: removed errors of depracated for fusedLocationApi

Constantly received errors about my fusedLocationApi being depracated. Had to change the gradle build file and go about using the fusedLocation a different way as suggested in the google api documentations.
added coarse location. fine location for GPS permission to get perfeect location of user and request permission before being able to user.
Added autocomplete into the places initialiser but haven't added any functionality for it. I will have to make it get to the location when the user clicks on it.

## Blog Entry 9. Title: Adding dialog builder and request code

added a dialog builder to interact with the user if they are successful or not with their request
added a request code, 1 or 0, 1 for successful, 0 for unsuccesful

## Blog Entry 10. Title: Changed build gradle and added a geolocation to move camera to selected destination.

Changed the buid gradle to acquire more information and data from google play services.
Changed the onMapRead to accomodate fine location and request permission to be able to access.
It also sets the user's location as soon as the app is launched and sets the map type to normal.
Added a destination address longitude and latitude as well as the current loaction longitude and latitude of where the user is at.
This is going to be useful when it comes to the route being drawn between both loactions.
Added a geolocate method that uses the place autocomplete.
Once a place is selected, previously nothing happened.
But now the app will bring the user to that selected place on the map and add a place marker on where they selected.
This is for when the user wants to go from point A to point B.
Before the location can be changed, request for permission,if granted, proceed.
When the location is changing, acquire the new position of where the user is.
Keep displaying the LatLng location to show their current position and keep updating.
Move the camera to that specific location whether they stand still or move.
Added fragment in activityMaps  to accomodate autocomplete fragment.

## Blog Entry 11. Title: Added logos. No functionality yet. 

added a bicycle vector and a refresh vector. these will act as buttons. The aim is that each buttons can be pressed at any time but will only be effective after certain stages have been complete.
For the bicycle logo button to function, the aim is that the user would've already selected their destination. 
Then once the user presses the bicycle logo, a route will be drawn from the user's current location to the destination's location.
For the refresh logo, the aim will be that once clicked, the route should be cleared from the map as well as the place marker for the selected location that the user uses as their destination.
added a designed menu inflater that supports the map. Specific designs.

## Blog Entry 12 

I ran into an issue while I was getting ready to set up the directions element of my app. I have realised that the bluetooth, gps and directions need to be background services 
in the app rather than activities, because the app needs to be on and communicating with the Flora board even when the phone is locked. So my next task is to move my gps and bluetooth code 
to their own IntentService classes and then make a new directions Service. I have made good progress on the gps and bluetooth services, but still have more things to fix 
before they are properly functional.
I also added a bluetooth button that changes colour depending on if its connected to a device or not.

## Blog Entry 13

I have my directions background service connecting to the google directions web service, it returns a JSON file which I will need to parse to extract the information from.
So that is my next task.

## Blog Entry 14

I now have the app pulling directions from the google server, it comes in the form of a JSON which I am currently working on parsing and turning into usable data.
I am almost ready to start working on an algorithm for detecting when a user needs to turn and also one for updating the routes. It is difficult to test my app to se if I am
heading in the right direction as it is supposed to be used on the road, but I would be hoping to be able to road test a very rough version of it in a few weeks time.


## Blog Entry 15

I now have a functioning version of the app which I have tested by itself by walking a few routes and the GPS seems to hold up well and
it directs me to my destination without any problems. I have not however tested it with the flora board yet. This is because the weather has
been very bad lately and the hardware is very out in the open so it could be damaged by rain. As soon as i get a clear day I will test
the whole system together but I expect it to all work together well.
My next task is to test out the hardware accelerometer and GPS modules that come with the flora to see if they can be of use for my project.
The GPS module will be very difficult to test properly as it can only be used outdoors with a clear view of the sky, which is unfortunately
a big ask in Ireland.

## Blog Entry 16

At the moment I am working on send the latitudes and longitudes as strings from the app to the arduino, and then converting them
into floats for the next step where i will work on an algorithm to calculate the distance between two sets of lats and lngs.
I have almost completed this task but I am having some trouble with the confirmation data sent back from the arduino. As when
I send data to it, I wait for confirmation that it was received properly before I send the next data packet. I am encountering some
problems where for some reason it stops letting me write to the arduino so I only get half of the array sent. I have not been able to work
out why yet as it happens at seemingly random and unrelated times. I am hoping I can find a solution for it soon.

## Blog Entry 17

I have gotten all the constituent parts ready to put together for the final stage of my product. The flora's GPS is getting accurate
readings, my distance algorithm works, I can transfer a full array of latitudes and longitudes along with the direction (left or right)
over bluetooth, and I think I have interrupts working on the flora too which I think may be an important part of my design.
Today I am going to create a data flow/UML type diagram to assist me in the construction of my arduino program, as it has become
a little too complicated with the interrupts and different functionality between the tethered and untethered system. I feel putting
a little extra concentration into design will really benefit me in this part of the project.
I hope to be have a something close to the final product by the end of this week so that I can move on to completing my documentation
and testing.

## Blog Entry 18

My project is now functioning and most testing has been done but there is more to do. I also have some documentation written but have
more to do today, tomorrow and monday. I also still have to film the video walkthrough which will probably happen tomorrow morning when
there are less care on the road, assuming it not raining.

