/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.WTBLE901.activity;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.android.WTBLE901.data.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import wtzn.wtbtble901.R;

import static com.example.android.WTBLE901.Utils.Norm;
import static com.example.android.WTBLE901.Utils.join;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private int mConnectionState = STATE_DISCONNECTED;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mRecording;
    private boolean mConnected;
    private boolean bFirstConnect;
    
    private byte[] cell = new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x64, 0x00};
    private byte[] mDeviceID = new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x68, 0x00};

    private Data mData;
    private char cSetTimeCnt = 0;

    private UICallback mUICallback;


    public interface UICallback {
        void handleBLEData(Data data);
        void onConnected(String deviceName);
        void onDisconnected();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {//Device Connected
                mConnected = true;
                bFirstConnect = true;
                if (mUICallback != null)
                    mUICallback.onConnected(mDeviceName);
                writeByes(cell);
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {//Device Disconnected
                mConnected = false;
                bFirstConnect = false;
                connect(mDeviceAddress);
                if (mUICallback != null)
                    mUICallback.onDisconnected();
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] byteIn = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                handleBLEData(byteIn);
                if (mUICallback != null)
                    mUICallback.handleBLEData(mData);
            }
        }
    };


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothLeService.this.getWorkableGattServices(getSupportedGattServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            byte[] data = characteristic.getValue();
            String sdata = "";
//            for (int i = 0; i < data.length; i++) {
//                sdata = sdata + String.format("%02x", (0xff & data[i]));
//            }
//            Log.e("--", "onCharacteristicRead = " + sdata);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "--onCharacteristicRead called--");
                byte[] sucString = characteristic.getValue();
                String string = new String(sucString);
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();
//            String sdata = "";
//            for (int i = 0; i < data.length; i++) {
//                sdata = sdata + String.format("%02x", (0xff & data[i]));
//            }
//            Log.e("--", "onCharacteristicWrite = " + sdata);
        }
    };

    @SuppressLint("DefaultLocale")
    public void handleBLEData(byte[] packBuffer) {
        StringBuilder sdata = new StringBuilder();
        for (byte aPackBuffer : packBuffer) {
            sdata.append(String.format("%02x", (0xff & aPackBuffer)));
        }

        Log.e("--", "handleBLEData = " + sdata);

        if (packBuffer.length == 20) {
            mData = Data.fromBytes(packBuffer);
            if (mRecording)
                writeDataToFile();
        } else {
            mData = Data.dummyData();
        }

        if(bFirstConnect)
            updateTime();
    }

    public void updateTime() {
        if (cSetTimeCnt < 4) {
            switch (cSetTimeCnt) {
                case 0:
                    writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x30, (byte) mData.getTime().get(Calendar.MONTH), (byte) (mData.getTime().get(Calendar.YEAR) - 2000)});
                    break;
                case 1:
                    writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x31, (byte) mData.getTime().get(Calendar.HOUR_OF_DAY), (byte) mData.getTime().get(Calendar.DAY_OF_MONTH)});
                    break;
                case 2:
                    writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x32, (byte) mData.getTime().get(Calendar.SECOND), (byte) mData.getTime().get(Calendar.MINUTE)});
                    break;
                case 3:
                    writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
                    break;
            }
            cSetTimeCnt++;
        }

    }

    private void writeDataToFile() {

        StringBuilder sb = new StringBuilder();
        sb.append(mData.getFormattedTime());
        for (float val : mData.getAllSensorData())
            sb.append(String.format(" %.3f", val));

        try {
            myFile.Write(sb.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    public float fAngleRef;
    float Yaw;

    public void SetAngleRef(boolean bRef) {
        if (bRef) fAngleRef = Yaw;
        else fAngleRef = 0;
    }

    ;

    public boolean getRssiVal() {
        if (mBluetoothGatt == null)
            return false;
        return mBluetoothGatt.readRemoteRssi();
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] packBuffer = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, packBuffer);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
//                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
//            Log.e(TAG, "Unable to obtain acc BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create acc new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    public boolean writeByes(byte[] bytes) {
        if (mNotifyCharacteristic != null) {
            Log.d("BLE", "WriteByte");
            mNotifyCharacteristic.setValue(bytes);
            return mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
        } else {
            Log.d("BLE", "NOCharacter");
            return false;
        }
    }

    public boolean writeString(String s) {
        if (mNotifyCharacteristic != null) {
            mNotifyCharacteristic.setValue(s);
            return mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
        } else {
            return false;
        }
    }

    private void getWorkableGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (uuid.toLowerCase().contains("ffe9")) {//Write
                    mNotifyCharacteristic = gattCharacteristic;
                    setCharacteristicNotification(mNotifyCharacteristic, true);
                }
                if (uuid.toLowerCase().contains("ffe4")) {//Read
                    setCharacteristicNotification(gattCharacteristic, true);
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
    }

    MyFile myFile;

    class MyFile {
        FileOutputStream fout;
        File path;

        public MyFile(File file) throws FileNotFoundException {
            this.path = file;
            fout = new FileOutputStream(file, false);
        }

        public void Write(String str) throws IOException {
            byte[] bytes = str.getBytes();
            fout.write(bytes);
        }

        public void Close() throws IOException {
            fout.close();
            fout.flush();
        }

        public void Mark() {
            try {
                fout.write("\r\nmark".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateFname() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
        Date date = new Date();
        return "Recording__" + dateFormat.format(new Date()) + ".txt";
    }

    public void toggleRecording() {
        if (!mRecording) {
            mRecording = true;

            try {
                myFile = new MyFile(new File(getExternalFilesDir(null), generateFname()));
                myFile.Write("date " + join(" ", Data.getAllSensorDataNames()) + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mRecording = false;
            try {
                myFile.Close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRecording() {
        return mRecording;
    }

    public void setUICallback(UICallback callback) {
        mUICallback = callback;
    }
}
