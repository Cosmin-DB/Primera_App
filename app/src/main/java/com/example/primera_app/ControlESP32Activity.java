package com.example.primera_app;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ControlESP32Activity extends AppCompatActivity implements NotificationReceiver.Receiver {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    protected NotificationReceiver mReceiver;
    protected BluetoothLeService mBLEService;
    private TextView mFotoResistorValue;
    private Switch mLED;
    private Switch mNotifyFotoResistor;
    protected boolean mBound = false;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_esp32);
        mFotoResistorValue=(TextView) findViewById(R.id.fotoResistorValue);
        mNotifyFotoResistor=(Switch) findViewById(R.id.switchFotoResistorNotify);
        mLED=(Switch) findViewById(R.id.switchLed);

        mLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mBLEService.updateLed((byte)1);
                }
                else {
                    mBLEService.updateLed((byte)0);
                }
            }
        });

        mNotifyFotoResistor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mBLEService.setNotifyFotoResistor(true);
                }
                else {
                    mBLEService.setNotifyFotoResistor(false);
                }
            }
        });

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mReceiver=new NotificationReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        // Bind to LocalService
        Intent BluetoothLeServiceIntent = new Intent(this, BluetoothLeService.class);

        BluetoothLeServiceIntent.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(BluetoothLeServiceIntent);
        }
        else {
            startService(BluetoothLeServiceIntent);
        }
        bindService(BluetoothLeServiceIntent, connection, Context.BIND_AUTO_CREATE);
    }
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            mBLEService = binder.getService();
            mBLEService.setmReceiver(mReceiver);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        int fotoResistorData =  resultData.getInt("data");
        mFotoResistorValue.setText(String.valueOf(fotoResistorData));
    }
}