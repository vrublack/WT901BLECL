package com.example.android.WTBLE901;

public class Utils {

    public static float Norm(float[] x) {
        float fSum = 0;
        for (int i = 0; i < x.length; i++) fSum += x[i] * x[i];
        return (float) Math.sqrt(fSum);
    }

    public static String join(String delim, String[] strs) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            result.append(strs[i]);
            if (i < strs.length - 1)
                result.append(delim);
        }
        return result.toString();
    }
}
