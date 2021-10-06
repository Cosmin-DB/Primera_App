package com.example.primera_app;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class DeviceScanActivity extends Activity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private BluetoothLeScanner bluetoothLeScanner;// = bluetoothAdapter.getBluetoothLeScanner();
    private Handler mHandler;
    ListView deviceList;
    Button  scanButton;
    private static final long SCAN_PERIOD = 10000; // in ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_scan_activity);
        deviceList = (ListView) findViewById(R.id.deviceList);
        mLeDeviceListAdapter = new LeDeviceListAdapter(getApplicationContext());
        deviceList.setAdapter(mLeDeviceListAdapter);

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(DeviceScanActivity.this, ControlESP32Activity.class);
                intent.putExtra(ControlESP32Activity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(ControlESP32Activity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                scanDevice(false);
                startActivity(intent);
            }
        });

        scanButton=findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mLeDeviceListAdapter.clear();
                scanDevice(true);
            }
        });

        mHandler = new Handler();
        //Assume the device has BLE, so:
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Checks permission
        PermissionUtils.askForPermissions(this);

        if (mBluetoothAdapter == null) {
            //modify with some user-friendly message
            Toast.makeText(this,"An error occurred while obtaining BluetoothAdapter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothLeScanner= mBluetoothAdapter.getBluetoothLeScanner();
        scanDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLeDeviceListAdapter.clear();
    }


    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice bluetoothDevice = result.getDevice();
            mLeDeviceListAdapter.addDevice(bluetoothDevice);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }
    };


    private void scanDevice(final boolean enable) {
        if (enable && !mScanning) {
            // Stops scanning after a predefined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mScanCallback);//Program a heandler to stop the scanner.
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }
}