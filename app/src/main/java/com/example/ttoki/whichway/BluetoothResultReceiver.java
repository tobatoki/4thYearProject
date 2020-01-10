package com.example.ttoki.whichway;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

class BluetoothResultReceiver extends ResultReceiver
{
    private MapsActivity maps;

    public BluetoothResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        maps.onReceiveBluetoothUpdate(resultData);
    }
}
