package com.example.android.WTBLE901.data;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Data {
    private float[] acc = new float[]{0, 0, 0};  // accelerometer
    private float[] gyr = new float[]{0, 0, 0};  // gyroscope (angular velocity)
    private float[] angle = new float[]{0, 0, 0};
    private float[] magn = new float[]{0, 0, 0};  // magnetic field
    private float[] port = new float[]{0, 0, 0, 0};
    private float[] quaternion = new float[]{0, 0, 0, 0};
    private float battery;
    private float pressure;
    private float altitude;
    private float temperature;

    private Calendar time;


    private String deviceName;

    private Data() {

    }

    public float[] getAcc() {
        return acc;
    }

    public float[] getGyr() {
        return gyr;
    }

    public float[] getAngle() {
        return angle;
    }

    public float[] getMagn() {
        return magn;
    }

    public float[] getPort() {
        return port;
    }

    public float[] getQuaternion() {
        return quaternion;
    }

    public float getBattery() {
        return battery;
    }

    public float getPressure() {
        return pressure;
    }

    public float getAltitude() {
        return altitude;
    }

    public float getTemperature() {
        return temperature;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public static Data fromBytes(byte[] packBuffer) {

        // TODO this needs to be an update method because values don't arrive at once

        Data d = new Data();
        float[] fData = new float[9];

        switch (packBuffer[1]) {
            case 0x61:
                for (int i = 0; i < 9; i++) {
                    fData[i] = (((short) packBuffer[i * 2 + 3]) << 8) | ((short) packBuffer[i * 2 + 2] & 0xff);
                }

                for (int i = 0; i < 3; i++) d.acc[i] = (float) (fData[i] / 32768.0 * 16.0);
                for (int i = 3; i < 6; i++) d.gyr[i - 3] = (float) (fData[i] / 32768.0 * 2000.0);
                for (int i = 6; i < 9; i++) d.angle[i - 6] = (float) (fData[i] / 32768.0 * 180.0);
                break;
            case 0x71:
                if (fData[2] != 0x68) {
                    for (int i = 0; i < 8; i++) {
                        fData[i] = (((short) packBuffer[i * 2 + 5]) << 8) | ((short) packBuffer[i * 2 + 4] & 0xff);
                    }
                }

                switch (packBuffer[2]) {
                    case 0x3A:
                        System.arraycopy(fData, 0, d.magn, 0, 3);
                        break;
                    case 0x45:
                        d.pressure = ((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff));
                        d.altitude = (((((long) packBuffer[11]) << 24) & 0xff000000) | ((((long) packBuffer[10]) << 16) & 0xff0000) | ((((long) packBuffer[9]) << 8) & 0xff00) | ((((long) packBuffer[8]) & 0xff))) / 100.0f;
                        break;
                    case 0x41:
                        for (int i = 0; i < 4; i++) d.port[i] = (float) (fData[i]);
                        break;
                    case 0x51:
                        for (int i = 0; i < 4; i++) d.quaternion[i] = (float) (fData[i] / 32768.0);
                        break;
                    case 0x40:
                        d.temperature = (float) (fData[0] / 100.0);
                        break;
                    case 0x64:
                        d.battery = fData[0];
                        break;
                    case 0x68:
                        try {
                            String s = new String(packBuffer, "ascii");
                            int end = s.indexOf("WT");
                            if (end == -1) {
                                d.deviceName = "";
                            } else {
                                d.deviceName = s.substring(4, end);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            default:
                break;
        }

        d.time = Calendar.getInstance();

        return d;
    }

    public static Data dummyData() {
        return new Data();
    }

    public float[] getAllSensorData() {
        return new float[]{acc[0], acc[1], acc[2], gyr[0], gyr[1], gyr[2], angle[0], angle[1], angle[2], magn[0], magn[1], magn[2],
                port[0], port[1], port[2], port[3], quaternion[0], quaternion[1], quaternion[2], quaternion[3], battery, pressure, altitude, temperature};
    }

    public static String[] getAllSensorDataNames() {
        return new String[] {
                "accX", "accY", "accZ", "gyrX", "gyrY", "gyrZ", "angleX", "angleY", "angleZ", "magnX", "magnY", "magnZ",
                "portX", "portY", "portZ", "portW", "quaternionX", "quaternionY", "quaternionZ", "quaternionW", "whatIsThisX", "whatIsThisY", "whatIsThisZ", "whatIsThisW", "pressure", "altitude", "temperature"
        };
    }

    public Calendar getTime() {
        return time;
    }

    public String getFormattedTime() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(time.getTime());
    }
}
