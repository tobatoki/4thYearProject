package com.example.ttoki.whichway;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class BluetoothLeService extends Service {
    private static final String TAG = "BluetoothLeService";
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private ResultReceiver resultReceiver;
    private Handler handler;
    private int currentState;
    private boolean scanning;
    private UUIDS uuids;
    private ArrayList<String> latiLongiTurnList;
    private int currentLatLngListIndex = 0;
    private String currentDirection = null;

    @Override // Function called when service is started from activity
    public int onStartCommand(Intent intent, int flags, int startId) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler();
        resultReceiver = intent.getParcelableExtra("resultReceiver");
        uuids = new UUIDS();
        //Scan for Bluetooth devices
        scanForDevices();

        //Register the LocalBroadcastManager receiver, that receives data from
        //the GPS service
        LocalBroadcastManager.getInstance(this).registerReceiver(directionsReceiver,
                new IntentFilter("GPS to Bluetooth"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopConnection();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(directionsReceiver);
        super.onDestroy();
    }

    //Broadcast receiver class that receives directions and the waypoints array
    private BroadcastReceiver directionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //If the message is a direction, call the sendDirection method
            //and send it the intent
            if(null != intent.getStringExtra("direction")) {
                String direction = intent.getStringExtra("direction");
                Log.d(TAG, "=======================In on receive in bluetooth and dir is " + direction);
                sendDirection(direction);
            }
            //If the message is a list of waypoints
            else if(null != intent.getExtras().getStringArrayList("LatLng")) {
                Log.d(TAG, "=======================In on receive in bluetooth waypoints");
                latiLongiTurnList = intent.getExtras().getStringArrayList("LatLng");

                //This initiates the sending of waypoints in the list, the zeroth
                //item is sent, and the program waits for confirmation from the arduino
                //in callback.onCharacteristicChanged() to send the next item in the list
                sendMessage(latiLongiTurnList.get(0));
            }
        }
    };

    //Choose which direction to send to sendMessage(String string)
    private void sendDirection(String direction) {
        if ("turn-left".equals(direction)) currentDirection = "lft";
        else if("turn-right".equals(direction)) currentDirection = "rgt";
        else if("n/a".equals(direction)) currentDirection = "n/a";
        else if("off".equals(direction)) currentDirection = "off";
        else if("finish".equals(direction)) currentDirection = "fin";
        sendMessage(currentDirection);
    }

    // This scans for bluetooth devices
    // If no bluetooth devices are found in specified time,
    // Ask for devices to be turned on
    // Then rescan
    void scanForDevices() {
        final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        final Bundle bluetoothBundle = new Bundle();
        final int SCAN_TIME = 10000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanning = false;
                bluetoothBundle.putString("No device", "No device detected, please turn on bluetooth device.");
                resultReceiver.send(0, bluetoothBundle);
            }
        }, SCAN_TIME);
        bluetoothBundle.putString("Scanning", "Scanning for devices...");
        scanning = true;
        bluetoothLeScanner.startScan(leScanCallback);
    }
    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes,
        // i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Bundle bluetoothBundle = new Bundle();
            currentState = newState;
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (!gatt.discoverServices()) {
                    Log.d(TAG, "Failed to Connect");
                    bluetoothBundle.putString("Failed to Connect", "Failed to Connect");
                    resultReceiver.send(0, bluetoothBundle);
                }
                else {
                    bluetoothBundle.putString("Connected", "Connected");
                    resultReceiver.send(0, bluetoothBundle);
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected");
                bluetoothBundle.putString("Disconnected", "Disconnected");
                resultReceiver.send(0, bluetoothBundle);
                scanForDevices();
            } else Log.d(TAG, "New State: " + newState);

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                Log.d(TAG, "Service discovery completed!");
            else
                Log.d(TAG, "Service discovery failed with status: " + status);
            // Save reference to each characteristic.
            tx = gatt.getService(uuids.getUartUuid()).getCharacteristic(uuids.getTxUuid());
            rx = gatt.getService(uuids.getUartUuid()).getCharacteristic(uuids.getRxUuid());
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true))
                Log.d(TAG, "Couldn't set notifications for RX characteristic!");
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(uuids.getClientUuid()) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(uuids.getClientUuid());
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc))
                    Log.d(TAG, "Couldn't write RX client descriptor value!");
            }
            else Log.d(TAG, "Couldn't get RX client descriptor!");
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Get the data sent from the arduino
            String receivedValue = characteristic.getStringValue(0);
            Log.d(TAG, "Received: " + receivedValue);
            Log.d(TAG, "");
            Log.d(TAG, "------------------------------------------------------------------");
            Log.d(TAG, "");
            //This is for sending the array of waypoints, it waits for a zero from the arduino
            //before sending the next waypoint
            if(receivedValue.equals("0") && currentLatLngListIndex < latiLongiTurnList.size() - 1)
                //Send the next waypoint in the list
                sendMessage(latiLongiTurnList.get(++currentLatLngListIndex));
        }
    };

    // This is for if the mobile device discovers the UUIDS or the
    // Flora board
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
            //If the UUIDs are not null
            if(null != result.getScanRecord().getServiceUuids()) {
                //Add UUIDs to a list
                List<ParcelUuid> parcelUuidList = result.getScanRecord().getServiceUuids();
                //Iterate through the list
                for (ParcelUuid u : parcelUuidList) {
                    //If the UUID is equal to the Uart UUID from the UUID class
                    if (uuids.getUartUuid().toString().equals(u.getUuid().toString())) {
                        //Stop scanning
                        bluetoothLeScanner.stopScan(leScanCallback);
                        // Connect to the device.
                        // Control flow will now go to the callback functions when BTLE events occur.
                        gatt = result.getDevice().connectGatt(getApplication(), false, callback);
                    }
                }
            }
            else {Log.d(TAG, "No UUIDs found for device.");}

        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    void stopConnection() {
        if (gatt != null) {
            // disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }

    void sendMessage(String message) {
        //If there's no tx signal, exit function
        if (tx == null) return;
        // Update TX characteristic value.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        //Send the message
        if (gatt.writeCharacteristic(tx))
            Log.d(TAG, "Sent: " + message);
        else Log.d(TAG, "Couldn't write message: " + message);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}