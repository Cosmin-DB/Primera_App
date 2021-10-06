package com.example.primera_app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.FileOutputStream;
/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    protected  BluetoothManager mBluetoothManager;
    protected  BluetoothAdapter mBluetoothAdapter;
    protected  BluetoothGatt mBluetoothGatt;
    protected BleGattCallback mGattCallback;
    protected IBinder binder ;
    protected ResultReceiver mReceiver;
    private String mDeviceAddress;

    public BluetoothLeService() {
        binder = new BluetoothLeService.LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //final Intent intent = getIntent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        mDeviceAddress = intent.getStringExtra(ControlESP32Activity.EXTRAS_DEVICE_ADDRESS);

        connect();
        setCustomListener();
        return START_NOT_STICKY;
    }



    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        mGattCallback.close();
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();

    }


    public boolean connect() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null|| mDeviceAddress == null) {
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        if (device == null) {
            return false;
        }
        mGattCallback=new BleGattCallback(getApplicationContext(), mBluetoothAdapter );
        mBluetoothGatt = device.connectGatt(this, false,mGattCallback);
        mGattCallback.setmBluetoothGatt(mBluetoothGatt);

        return true; // Implement logic for successful conected
    }


    private void setCustomListener(){
        mGattCallback.setCustomListener(new BleEventsToServiceListener() {
            public void dataReady(int data){
                Bundle b= new Bundle();
                b.putInt("data",data);
                mReceiver.send(0,b);
            }
        });
    }
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothLeService.this;
        }
    }

    public void updateLed(byte state){
        mGattCallback.updateLed(state);
    }

    public void setNotifyFotoResistor(boolean value){
        mGattCallback.setNotifyFotoResistor(value);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotificationChannel() {

        Intent notificationIntent = new Intent(this, BluetoothLeService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        // A Foreground service must provide a notification for the status bar.
        Notification notification = new Notification.Builder(this, "Service ESP32")
                .setContentTitle("InsoleService")
                .setContentText("El servicio de monitorización de la marcha se está ejecutando en segundo plano")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(101, notification);
        NotificationChannel serviceChannel = new NotificationChannel(
                "Service ESP32",
                "InsoleService",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    public void setmReceiver(ResultReceiver mReceiver) {
        this.mReceiver = mReceiver;
    }



}