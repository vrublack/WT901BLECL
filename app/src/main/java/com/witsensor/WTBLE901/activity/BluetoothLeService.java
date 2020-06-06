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

package com.witsensor.WTBLE901.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.witsensor.WTBLE901.data.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.witsensor.WTBLE901.R;
import com.witsensor.WTBLE901.data.Statistics;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final String KEY_NOTE = "key_note";

    public static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID_STATUS = 1;
    private static final int NOTIFICATION_ID_INTEGRITY = 2;

    private static final CharSequence CHARACTERISTIC_WRITE = "ffe9";
    private static final CharSequence CHARACTERISTIC_READ = "ffe4";
    private static final String CHARACTERISTIC_READ_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    public static boolean isRunning = false;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    // attributes for every device
    private Map<String, BluetoothGatt> mBluetoothGatt = new HashMap<>();
    private Map<String, BluetoothGattCharacteristic> mNotifyCharacteristic = new HashMap<>();
    private Map<String, Boolean> mConnected = new HashMap<>();
    private Map<String, Data> mData = new HashMap<>();
    private Map<String, MyFile> mFile = new HashMap<>();
    private Map<String, Statistics> mStats = new HashMap<>();

    private boolean mManuallyDisconnected;  // to catch a bug where the device somehow gets reconnected
    private boolean mRecording;
    private boolean mRecordingBaseline;

    private long lastIntegrityCheckTime = System.currentTimeMillis();

    public static byte[] cell = new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x64, 0x00};
    public static byte[] mDeviceID = new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x68, 0x00};

    private char cSetTimeCnt = 0;

    private UICallback mUICallback;

    public boolean setRateAll(int iOutputRate) {
        boolean success = true;
        for (String device : mBluetoothGatt.keySet())
            success = success && setRate(device, iOutputRate);
        return success;
    }

    public boolean setRate(String device, int iOutputRate) {
        return writeByes(device, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x03, (byte) iOutputRate, (byte) 0x00});
    }

    public void addMark(String device, String note) {
        if (mRecording && mFile.containsKey(device) && !mFile.get(device).isClosed())
            mFile.get(device).mark(note);
    }

    public void addMarkAll(String note) {
        if (!mRecording)
            return;

        for (MyFile file : mFile.values()) {
            if (!file.isClosed())
                file.mark(note);
        }
    }

    public File getPath(String device) {
        return mFile.containsKey(device) ? mFile.get(device).path : null;
    }


    public interface UICallback {
        void handleBLEData(String deviceAddress, Data data);

        void onConnected(String deviceAddress);

        void onDisconnected(String deviceAddress);
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final String device = gatt.getDevice().getAddress();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!mManuallyDisconnected) {
                    mConnected.put(device, true);

                    if (!mData.containsKey(device))
                        mData.put(device, new Data());

                    if (!mStats.containsKey(device))
                        mStats.put(device, new Statistics());
                    else
                        mStats.get(device).resume();

                    if (mUICallback != null)
                        mUICallback.onConnected(device);

                    writeByes(device, cell);

                    updateStatusNotification();

                    Log.i(TAG, "Connected to GATT server.");
                    Log.i(TAG, "Attempting to start service discovery:" +
                            gatt.discoverServices());
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mConnected.put(device, false);
                mNotifyCharacteristic.remove(device);
                mStats.get(device).pause();

                if (mUICallback != null)
                    mUICallback.onDisconnected(device);

                updateStatusNotification();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String device = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothLeService.this.getWorkableGattServices(device, getSupportedGattServices(device));
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();
            String device = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleBLEData(device, data);
                if (mUICallback != null)
                    mUICallback.handleBLEData(device, mData.get(device));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            String device = gatt.getDevice().getAddress();
            handleBLEData(device, data);
            if (mUICallback != null)
                mUICallback.handleBLEData(device, mData.get(device));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }
    };

    private void updateStatusNotification() {
        int connected = 0;

        for (boolean isConnected : mConnected.values()) {
            if (isConnected)
                connected++;
        }

        if (mRecording)
            passNotification(String.format(getString(R.string.recording_status), connected, mConnected.size()), NOTIFICATION_ID_STATUS, true);
        else
            passNotification(String.format(getString(R.string.not_recording_status), connected, mConnected.size()), NOTIFICATION_ID_STATUS, false);
    }

    private void updateIntegrityNotification(int devicesFailed) {
        if (devicesFailed == 0) {
            removeNotification(NOTIFICATION_ID_INTEGRITY);
        } else {
            passNotification(String.format(getString(R.string.integrity_check_message), devicesFailed), NOTIFICATION_ID_INTEGRITY, false);
        }
    }

    @SuppressLint("DefaultLocale")
    private void handleBLEData(String device, byte[] packBuffer) {
        mStats.get(device).logSampleReceived();

        // do this check every 30 minutes
        if (System.currentTimeMillis() - lastIntegrityCheckTime > 30 * 60 * 1000L) {
            checkIntegrity();
            lastIntegrityCheckTime = System.currentTimeMillis();
        }

        StringBuilder sdata = new StringBuilder();
        for (byte aPackBuffer : packBuffer) {
            sdata.append(String.format("%02x", (0xff & aPackBuffer)));
        }

        String formatted;
        if (packBuffer.length == 20) {
            formatted = mData.get(device).update(packBuffer);
            if (mRecording && formatted != null) {
                if (!mFile.containsKey(device))
                    createNewFile(device);
                try {
                    mFile.get(device).write(formatted);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        updateTime(device);
    }

    private void checkIntegrity() {
        Map<String, Double> samplesPerSecond = new HashMap<>();
        double maxSamplesPerSecond = -1;
        for (Map.Entry<String, Statistics> entry : mStats.entrySet()) {
            samplesPerSecond.put(entry.getKey(), entry.getValue().getSamplesPerSecond());
            maxSamplesPerSecond = Math.max(maxSamplesPerSecond, samplesPerSecond.get(entry.getKey()));
        }

        int devicesFailed = 0;
        for (String device : samplesPerSecond.keySet()) {
            if (samplesPerSecond.get(device) < maxSamplesPerSecond * 0.8) {
                devicesFailed++;
            }
        }

        updateIntegrityNotification(devicesFailed);
    }


    public void updateTime(String device) {
        Calendar t = mData.get(device).getTime();
        if (cSetTimeCnt < 4) {
            switch (cSetTimeCnt) {
                case 0:
                    writeByes(device, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x30, (byte) t.get(Calendar.MONTH), (byte) (t.get(Calendar.YEAR) - 2000)});
                    break;
                case 1:
                    writeByes(device, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x31, (byte) t.get(Calendar.HOUR_OF_DAY), (byte) t.get(Calendar.DAY_OF_MONTH)});
                    break;
                case 2:
                    writeByes(device, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x32, (byte) t.get(Calendar.SECOND), (byte) t.get(Calendar.MINUTE)});
                    break;
                case 3:
                    writeByes(device, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
                    break;
            }
            cSetTimeCnt++;
        }

    }


    public float fAngleRef;
    float Yaw;

    public void SetAngleRef(boolean bRef) {
        if (bRef) fAngleRef = Yaw;
        else fAngleRef = 0;
    }

    public boolean getRssiVal(String device) {
        if (mBluetoothGatt.get(device) == null)
            return false;
        return mBluetoothGatt.get(device).readRemoteRssi();
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
    public void onDestroy() {
        super.onDestroy();
        disconnectAll(true);
        removeNotification(NOTIFICATION_ID_STATUS);
        removeNotification(NOTIFICATION_ID_INTEGRITY);
        unregisterReceiver(mNoteReceiver);
        isRunning = false;
    }

    private final IBinder mBinder = new LocalBinder();

    private boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain acc BluetoothAdapter.");
            return false;
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_INSERT);
        registerReceiver(mNoteReceiver, intentFilter);

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        mConnected.put(address, false); // for now, will be changed to true if it connects successfully

        mManuallyDisconnected = false;

        if (mBluetoothGatt.get(address) != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.get(address).connect();
        } else {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            mBluetoothGatt.put(address, device.connectGatt(this, true, mGattCallback));
            Log.d(TAG, "Trying to create acc new connection.");
        }

        return true;
    }

    public boolean connectAll(final Set<String> addresses) {
        boolean success = false;
        for (String address : addresses) {
            success = connect(address) || success;
        }
        return success;
    }

    public void disconnect(String device, boolean releaseResources) {
        if (!mConnected.get(device))
            return;
        if (mBluetoothAdapter == null || mBluetoothGatt.get(device) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mManuallyDisconnected = true;
        if (releaseResources) {
            mGattCallback.onConnectionStateChange(mBluetoothGatt.get(device), 0, BluetoothProfile.STATE_DISCONNECTED);
            mBluetoothGatt.get(device).close();
            mBluetoothGatt.remove(device);
        } else {
            mBluetoothGatt.get(device).disconnect();
        }
        mConnected.put(device, false);
    }

    public void disconnectAll(boolean releaseResources) {
        for (String address : mConnected.keySet()) {
            disconnect(address, releaseResources);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        // TODO abort when failed
        initialize();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.sensor_status_channel);
            String description = getString(R.string.sensor_status_channel);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(NOTIFICATION_ID_STATUS, getNotification(getString(R.string.not_connected), false));

        return START_NOT_STICKY;
    }

    public class NoteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput == null) {
                return;
            }

            addMarkAll(remoteInput.getCharSequence(KEY_NOTE).toString());

            updateStatusNotification();
        }
    }

    private NoteReceiver mNoteReceiver = new NoteReceiver();

    private Notification getNotification(String msg, boolean replyAction) {
        Intent notificationIntent = new Intent(this, DeviceControlActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent);

        if (replyAction) {
            RemoteInput remoteInput = new RemoteInput.Builder(KEY_NOTE)
                    .setLabel(getResources().getString(R.string.mark))
                    .build();
            Intent intent = new Intent(Intent.ACTION_INSERT);
            PendingIntent replyPendingIntent =
                    PendingIntent.getBroadcast(getApplicationContext(),
                            5,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Action action =
                    new NotificationCompat.Action.Builder(R.drawable.ic_check,
                            getString(R.string.mark), replyPendingIntent)
                            .addRemoteInput(remoteInput)
                            .build();

            b = b.addAction(action);
        }


        return b.build();
    }

    private void passNotification(String msg, int id, boolean replyAction) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.notify(id, getNotification(msg, replyAction));
    }

    private void removeNotification(int id) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public void readCharacteristic(String device, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt.get(device) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.get(device).readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(String device, BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt.get(device) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.get(device).setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(String device, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt.get(device) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.get(device).writeCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices(String device) {
        if (mBluetoothGatt.get(device) == null) return null;
        return mBluetoothGatt.get(device).getServices();
    }

    public boolean writeByes(String device, byte[] bytes) {
        if (mNotifyCharacteristic.get(device) != null && mBluetoothGatt.get(device) != null) {
            mNotifyCharacteristic.get(device).setValue(bytes);
            return mBluetoothGatt.get(device).writeCharacteristic(mNotifyCharacteristic.get(device));
        } else {
            return false;
        }
    }

    public boolean writeString(String device, String s) {
        if (mNotifyCharacteristic.get(device) != null && mBluetoothGatt.get(device) != null) {
            mNotifyCharacteristic.get(device).setValue(s);
            return mBluetoothGatt.get(device).writeCharacteristic(mNotifyCharacteristic.get(device));
        } else {
            return false;
        }
    }

    private void getWorkableGattServices(final String device, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (uuid.toLowerCase().contains(CHARACTERISTIC_WRITE)) {//write
                    mNotifyCharacteristic.put(device, gattCharacteristic);
                    setCharacteristicNotification(device, gattCharacteristic, true);

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!setRate(device, getSharedPreferences("Output", Activity.MODE_PRIVATE).getInt("Rate", 6))) {
                                Log.e(TAG, "Failed to set rate");
                            }
                        }
                    }, 1000);
                }

                if (uuid.toLowerCase().contains(CHARACTERISTIC_READ)) {//Read
                    setCharacteristicNotification(device, gattCharacteristic, true);
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_READ_DESCRIPTOR));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.get(device).writeDescriptor(descriptor);
                }
            }
        }
    }

    class MyFile {
        FileOutputStream fout;
        File path;
        boolean closed = false;

        public MyFile(File file) throws FileNotFoundException {
            this.path = file;
            fout = new FileOutputStream(file, false);
        }

        public void write(String str) throws IOException {
            byte[] bytes = str.getBytes();
            fout.write(bytes);
        }

        public void close() throws IOException {
            fout.close();
            fout.flush();
            closed = true;
        }

        public void mark(String note) {
            try {
                fout.write(("mark " + note + "\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isClosed() {
            return closed;
        }

        public void startBaseline() {
            try {
                fout.write(("baseline start\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void endBaseline() {
            try {
                fout.write(("baseline end\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateFname(String device) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
        return "Recording__" + dateFormat.format(new Date()) + "__" + device.replace(":", "-") + ".txt";
    }

    public void toggleRecording() {
        if (!isAnyConnected())
            return;

        if (!mRecording) {
            mFile.clear();
            mRecording = true;
        } else {
            mRecording = false;

            for (MyFile file : mFile.values()) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        updateStatusNotification();
    }

    /**
     * Records baseline of sensors in some sort of initial state
     */
    public void toggleRecordingBaseline() {
        if (!mRecording)
            return;

        if (!mRecordingBaseline) {
            mRecordingBaseline = true;

            for (MyFile file : mFile.values()) {
                if (!file.isClosed())
                    file.startBaseline();
            }

        } else {
            mRecordingBaseline = false;

            for (MyFile file : mFile.values()) {
                if (!file.isClosed())
                    file.endBaseline();
            }
        }
    }

    public void createNewFile(String device) {
        try {
            mFile.put(device, new MyFile(new File(getExternalFilesDir(null), generateFname(device))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return mRecording;
    }

    public boolean isRecordingBaseline() {
        return mRecordingBaseline;
    }

    public boolean isAnyConnected() {
        for (boolean connected : mConnected.values()) {
            if (connected)
                return true;
        }

        return false;
    }

    public boolean isConnected(String device) {
        return mConnected.containsKey(device) && mConnected.get(device);
    }

    public void setUICallback(UICallback callback) {
        mUICallback = callback;
    }
}
