package com.example.primera_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;

public class BleGattCallback extends BluetoothGattCallback {
    protected final String TAG = "BleGattCallback";
    public static final String LIGHT_BASE_UUID =           "6a9d6db8-7dbe-4ae1-a5bc-b4e55a2d73d";
    public static final String LIGHT_SERVICE_UUID=               LIGHT_BASE_UUID+"0";
    public static final String LED_CHARACTERISTIC_UUID=          LIGHT_BASE_UUID+"1";
    public static final String FOTORESISTOR_CHARACTERISTIC_UUID= LIGHT_BASE_UUID+"2";
    protected static final String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    private  final Queue<Object> BleQueue = new LinkedList<>();
    private  BluetoothAdapter mBluetoothAdapter;
    private  BluetoothGatt mBluetoothGatt;
    private static BluetoothGattCharacteristic mLedCharacteristic;
    private static BluetoothGattCharacteristic mFotoResistorCharacteristic;
    private BleEventsToServiceListener listener;
    private Handler mHandler;
    private boolean mClose=false;


    public BleGattCallback(
            Context context,
            BluetoothAdapter mBluetoothAdapter)
    {

        this.mHandler = new Handler();
        this.mBluetoothAdapter=mBluetoothAdapter;
        this.listener=null;
    }
    public void setCustomListener(BleEventsToServiceListener listener) {
        this.listener = listener;
    }

    /**
     * This is called on a connection state change (either connection or disconnection)
     *
     * @param gatt     The GATT database object
     * @param status   Status of the event
     * @param newState New state (connected or disconnected)
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            mBluetoothGatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Disconnected from GATT server.");
            if(mClose){
                Log.d(TAG, "mClose="+mClose+" end of BleGattCallback");
                mBluetoothGatt.close();
            }else{
                reconnect();
            }
        }
    }
    public void close(){
        Log.d(TAG, "Close BleGattCallback...");
        mClose=true;
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
    }
    /**
     * This is called when service discovery has completed.
     * It broadcasts an update to the main activity.
     *
     * @param gatt   The GATT database object
     * @param status Status of whether the discovery was successful.
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(LIGHT_SERVICE_UUID));
            if (gattService == null) return; // return if the PSM service is not supported
            mLedCharacteristic = gattService.getCharacteristic(UUID.fromString(LED_CHARACTERISTIC_UUID));
            mFotoResistorCharacteristic= gattService.getCharacteristic(UUID.fromString(FOTORESISTOR_CHARACTERISTIC_UUID));
            setCharacteristicNotification(mFotoResistorCharacteristic, true);

        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
        }
    }

    /**
     * This is called when a characteristic with notify set changes.
     * It broadcasts an update to the main activity with the changed data.
     *
     * @param gatt           The GATT database object
     * @param characteristic The characteristic that was changed
     */
    @Override
    public  void onCharacteristicChanged(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic) {
        listener.dataReady(characteristic.getIntValue(FORMAT_UINT16,0));
    }

    /**
     * This handles the BLE Queue. If the queue is not empty, it starts the next event.
     */
    private void handleBleQueue() {
        if (BleQueue.size() > 0) {
            // Determine which type of event is next and fire it off
            if (BleQueue.element() instanceof BluetoothGattDescriptor) {
                mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) BleQueue.element());
            } else if (BleQueue.element() instanceof BluetoothGattCharacteristic) {
                mBluetoothGatt.writeCharacteristic((BluetoothGattCharacteristic) BleQueue.element());
            }
        }
    }

    /**
     * This is called when a characteristic write has completed. Is uses a queue to determine if
     * additional BLE actions are still pending and launches the next one if there are.
     *
     * @param gatt           The GATT database object
     * @param characteristic The characteristic that was written.
     * @param status         Status of whether the write was successful.
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic,
                                      int status) {
        // Pop the item that was written from the queue
        BleQueue.remove();
        // See if there are more items in the BLE queues
        handleBleQueue();
    }

    /**
     * This is called when a CCCD write has completed. It uses a queue to determine if
     * additional BLE actions are still pending and launches the next one if there are.
     *
     * @param gatt       The GATT database object
     * @param descriptor The CCCD that was written.
     * @param status     Status of whether the write was successful.
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int status) {
        // Pop the item that was written from the queue
        BleQueue.remove();
        // See if there are more items in the BLE queues
        handleBleQueue();
    }

    @Override
    public void onReliableWriteCompleted (BluetoothGatt gatt,
                                          int status){
        Log.d( "listenThread","onReliableWriteCompleted");
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enable        If true, enable notification.  False otherwise.
     */
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                               boolean enable) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /* Enable or disable the callback notification on the phone */
       boolean result= mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
        //UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        mBluetoothGatt.writeDescriptor(descriptor);
       Log.d(TAG, "Result:"+result);

    }

    private void reconnect(){
        boolean result =mBluetoothGatt.connect();
        Log.d(TAG, "Try to reconnect: "+result+" mBluetoothGatt is:"+mBluetoothGatt.toString());
        if(!result) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    reconnect();
                }
            }, 500);
        }
    }

    public void setmBluetoothGatt(BluetoothGatt mBluetoothGatt) {
        this.mBluetoothGatt = mBluetoothGatt;
    }

    public void updateLed(byte state)
    {
        if (mLedCharacteristic != null) {
            mLedCharacteristic.setValue(state, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            writeCharacteristic(mLedCharacteristic);
        }
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic The characteristic to write.
     */
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        BleQueue.add(characteristic);
        if (BleQueue.size() == 1) {
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    public void setNotifyFotoResistor(boolean value){
        setCharacteristicNotification(mFotoResistorCharacteristic, value);
    }
}
