package com.witsensor.WTBLE901.data;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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

    public String update(byte[] packBuffer) {

        time = Calendar.getInstance();

        float[] fData = new float[9];

        switch (packBuffer[1]) {
            case 0x61:
                for (int i = 0; i < 9; i++) {
                    fData[i] = (((short) packBuffer[i * 2 + 3]) << 8) | ((short) packBuffer[i * 2 + 2] & 0xff);
                }

                for (int i = 0; i < 3; i++) acc[i] = (float) (fData[i] / 32768.0 * 16.0);
                for (int i = 3; i < 6; i++) gyr[i - 3] = (float) (fData[i] / 32768.0 * 2000.0);
                for (int i = 6; i < 9; i++) angle[i - 6] = (float) (fData[i] / 32768.0 * 180.0);

                return String.format(Locale.US, "acc,%s,%.4f,%.4f,%.4f\ngyr,%s,%.4f,%.4f,%.4f\nangle,%s,%.4f,%.4f,%.4f\n", getFormattedTime(),
                        acc[0], acc[1], acc[2], getFormattedTime(), gyr[0], gyr[1], gyr[2], getFormattedTime(), angle[0], angle[1], angle[2]);

            case 0x71:
                if (fData[2] != 0x68) {
                    for (int i = 0; i < 8; i++) {
                        fData[i] = (((short) packBuffer[i * 2 + 5]) << 8) | ((short) packBuffer[i * 2 + 4] & 0xff);
                    }
                }

                switch (packBuffer[2]) {
                    case 0x3A:
                        System.arraycopy(fData, 0, magn, 0, 3);
                        return String.format(Locale.US, "magn,%s,%.4f,%.4f,%.4f\n", getFormattedTime(), magn[0], magn[1], magn[2]);
                    case 0x45:
                        pressure = ((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff));
                        altitude = (((((long) packBuffer[11]) << 24) & 0xff000000) | ((((long) packBuffer[10]) << 16) & 0xff0000) | ((((long) packBuffer[9]) << 8) & 0xff00) | ((((long) packBuffer[8]) & 0xff))) / 100.0f;
                        return String.format(Locale.US, "pressure,%s,%.4f\naltitude,%s,%.4f", getFormattedTime(), pressure, getFormattedTime(), altitude);
                    case 0x41:
                        for (int i = 0; i < 4; i++) port[i] = (float) (fData[i]);
                        return String.format(Locale.US, "port,%s,%.4f,%.4f,%.4f,%.4f\n", getFormattedTime(), port[0], port[1], port[2], port[3]);
                    case 0x51:
                        for (int i = 0; i < 4; i++) quaternion[i] = (float) (fData[i] / 32768.0);
                        return String.format(Locale.US, "quaternion,%s,%.4f,%.4f,%.4f,%.4f\n", getFormattedTime(), quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
                    case 0x40:
                        temperature = (float) (fData[0] / 100.0);
                        return String.format(Locale.US, "temp,%s,%.4f\n", getFormattedTime(), temperature);
                    case 0x64:
                        battery = fData[0];
                        return String.format(Locale.US, "battery,%s,%.4f\n", getFormattedTime(), battery);
                }
                break;
            default:
                break;
        }

        return null;
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
