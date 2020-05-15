package com.witsensor.WTBLE901;

public class Constants {
    public static byte[] REQUEST_ANGLE = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x40, (byte) 0x00};
    public static byte[] REQUEST_MAGN = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x3A, (byte) 0x00};
    public static byte[] REQUEST_PRESSURE = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x45, (byte) 0x00};
    public static byte[] REQUEST_PORT = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x41, (byte) 0x00};
    public static byte[] REQUEST_QUATER = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x51, (byte) 0x00};
}
