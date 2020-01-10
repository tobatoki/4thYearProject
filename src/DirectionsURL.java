package com.example.ttoki.whichway;

public class DirectionsURL {


    public String makeDirectionsURL(String origin, String destination){

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="+origin+"&destination="+destination+"&mode=bicycling&key=AIzaSyA_16XMwKNjCHR9YMhK2-LXnwU6kw0-RNs";
        return url;

    }
}
