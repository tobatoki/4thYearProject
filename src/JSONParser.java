package com.example.ttoki.whichway;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

public class JSONParser {

    private final String TAG = "JSONParser";
    private JSONObject jsonDirectionsObject;
    private LinkedList<Step> stepsList;
    private boolean listFull = false;
    private String routeLine;

    public JSONParser(String json) {
        try {
            jsonDirectionsObject = new JSONObject(json);
            parseJSON();
        }
        catch(JSONException e){}

    }

    public boolean getListFull()
    {
        return listFull;
    }

    public String getPolyline()
    {
        return routeLine;
    }

    LinkedList<Step> getStepsList()
    {
        return stepsList;
    }


    // Here is where we parse the JSONArray that we retrieve as the route
    // We want to only get the relevant information needed for th eproject
    // Whererever we see "distance, start_location. etc",
    // we want to retrieve the values that they have inside and store them

    private void parseJSON() {
        try {
            stepsList = new LinkedList<>();
            JSONArray routesJSON = jsonDirectionsObject.getJSONArray("routes");
            JSONArray legsJSON = routesJSON.getJSONObject(0).getJSONArray("legs");
            JSONArray stepsJSON = legsJSON.getJSONObject(0).getJSONArray("steps");
            routeLine = routesJSON.getJSONObject(0).getJSONObject("overview_polyline").getString("points");

            for(int i = 0; i < stepsJSON.length(); i++) {
                String distance = stepsJSON.getJSONObject(i).getJSONObject("distance").getString("value");
                String endLat = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lat");
                String endLng = stepsJSON.getJSONObject(i).getJSONObject("end_location").getString("lng");
                String startLat = stepsJSON.getJSONObject(i).getJSONObject("start_location").getString("lat");
                String startLng = stepsJSON.getJSONObject(i).getJSONObject("start_location").getString("lng");
                String maneuver;
                if(stepsJSON.getJSONObject(i).has("maneuver"))
                    maneuver = stepsJSON.getJSONObject(i).getString("maneuver");
                else maneuver = "n/a";
                stepsList.add(new Step(endLat, startLat, startLng, endLng, distance, maneuver));
            }
            listFull = true;
        }
        catch(JSONException e) {}
    }
}
