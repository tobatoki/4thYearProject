package com.example.ttoki.whichway;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

class DataRetrieval extends ResultReceiver
{
    private static final String TAG = "DataRetrieval";

    private MapsActivity mMap;

    public DataRetrieval(Handler handler, MapsActivity m) {
        super(handler);
        mMap = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        mMap.gpsRefresh(resultData.getString("latitude"), resultData.getString("longitude"),
                resultData.getString("routeLine"), resultData.getString("direction")
                ,resultData.getString("direction"));
    }
}