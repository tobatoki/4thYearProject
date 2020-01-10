package com.example.ttoki.whichway;

public class Step {

    private String endLat;
    private String startLat;
    private String startLng;
    private String endLng;
    private String distance;
    private String maneuver;

    public Step(String endLat, String startLat, String startLng, String endLng, String distance, String maneuver)
    {
        this.endLat = endLat;
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLng = endLng;
        this.distance = distance;
        this.maneuver = maneuver;
    }

    public String getManeuver() {
        return maneuver;
    }

    public String getEndLat() {
        return endLat;
    }

    public String getEndLng() {
        return endLng;
    }

    public String getDistance() {
        return distance;
    }

    public String getStartLat() { return startLat;}

    public String getStartLng() { return startLng;}

}