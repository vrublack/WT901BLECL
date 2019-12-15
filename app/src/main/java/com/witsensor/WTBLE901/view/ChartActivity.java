package com.witsensor.WTBLE901.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.witsensor.WTBLE901.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class ChartActivity extends Activity {

    private CombinedChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        mChart = findViewById(R.id.chart);
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);

        final String fname = setUpCharts();

        if (fname != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    saveToPath("chart_" + fname);
                }
            }, 2000);
        }
    }

    public boolean saveToPath(String title) {

        Bitmap b = mChart.getChartBitmap();

        OutputStream stream = null;
        try {
            stream = new FileOutputStream(getExternalFilesDir(null).getPath() + "/" + title
                    + ".png");

            /*
             * Write bitmap to file using JPEG or PNG and 40% quality hint for
             * JPEG.
             */
            b.compress(Bitmap.CompressFormat.PNG, 80, stream);

            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    private String setUpCharts() {
        File[] files = getExternalFilesDir(null).listFiles();

        List<Pair<Date, File>> allDates = new ArrayList<>();
        for (File f : files) {
            if (!f.getName().startsWith("Recording__"))
                continue;
            try {
                String datePortion = f.getName().substring(11, f.getName().length() - 4);
                Date date = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss").parse(datePortion);
                allDates.add(new Pair<Date, File>(date, f));

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        // sort by date
        Collections.sort(allDates, new Comparator<Pair<Date, File>>() {
            @Override
            public int compare(Pair<Date, File> t1, Pair<Date, File> t2) {
                return t1.first.compareTo(t2.first);
            }
        });

        if (allDates.size() < 2) {
            Toast.makeText(this, "Need calibration file", Toast.LENGTH_LONG).show();
            return null;
        }

        File calibrationFile = allDates.get(allDates.size() - 2).second;
        File dataFile = allDates.get(allDates.size() - 1).second;

        List<float[]> calibAngles = getAngles(calibrationFile);
        List<float[]> angles = getAngles(dataFile);

        ArrayList<Entry> xValues = new ArrayList<>();
        ArrayList<Entry> yValues = new ArrayList<>();
        ArrayList<Entry> zValues = new ArrayList<>();
        ArrayList<Entry> marks = new ArrayList<>();
        for (int i = 0; i < angles.size(); i++) {
            if (angles.get(i) == null) {
                if (xValues.size() > 0)
                    marks.add(new Entry(xValues.get(xValues.size() - 1).getX(), 0));
            } else {
                float seconds = angles.get(i)[0];
                xValues.add(new Entry(seconds, angles.get(i)[1]));
                yValues.add(new Entry(seconds, angles.get(i)[2]));
                zValues.add(new Entry(seconds, angles.get(i)[3]));
            }
        }

        float calibX = 0;
        float calibY = 0;
        float calibZ = 0;
        for (int i = 0; i < calibAngles.size(); i++) {
            if (calibAngles.get(i) == null)
                continue;
            calibX += calibAngles.get(i)[1];
            calibY += calibAngles.get(i)[2];
            calibZ += calibAngles.get(i)[3];
        }
        calibX /= calibAngles.size();
        calibY /= calibAngles.size();
        calibZ /= calibAngles.size();

        makeChart(xValues, yValues, zValues, calibX, calibY, calibZ, marks);

        return dataFile.getName();
    }

    private List<float[]> getAngles(File file) {
        List<float[]> result = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            float firstSeconds = -1;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.contains("mark")) {
                        result.add(null);
                    } else {
                        String[] comps = line.split(",");
                        if (!comps[0].equals("angle") || comps.length != 5)
                            continue;
                        String timeStr = comps[1];
                        float angleX = Float.parseFloat(comps[2]);
                        if (angleX < 0)
                            angleX = 360 + angleX;
                        float angleY = Float.parseFloat(comps[3]);
                        float angleZ = Float.parseFloat(comps[4]);
                        Date date = new SimpleDateFormat("HH:mm:ss.SSS").parse(timeStr);
                        float seconds = date.getTime() / 1000.f;
                        if (firstSeconds == -1)
                            firstSeconds = seconds;
                        seconds -= firstSeconds;
                        result.add(new float[]{seconds, angleX, angleY, angleZ});
                    }
                } catch (ParseException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void makeChart(List<Entry> x, List<Entry> y, List<Entry> z, float xCalib, float yCalib, float zCalib, List<Entry> marks) {
        if (x.size() == 0)
            return;
        ArrayList<ILineDataSet> calibData = new ArrayList<>();
        ArrayList<IScatterDataSet> actualData = new ArrayList<>();
        float minTime = x.get(0).getX();
        float maxTime = x.get(x.size() - 1).getX();
        float pSize = 3f;

        ScatterDataSet setX;
        setX = new ScatterDataSet(x, "x");
        setX.setDrawIcons(false);
        setX.setColor(Color.RED);
        setX.setValueTextSize(9f);
        setX.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        setX.setScatterShapeSize(pSize);
        actualData.add(setX);
        calibData.add(getCalibLine("x calib", Color.rgb(0xe0, 0, 0), xCalib, minTime, maxTime));

        ScatterDataSet setY;
        setY = new ScatterDataSet(y, "y");
        setY.setDrawIcons(false);
        setY.setColor(Color.GREEN);
        setY.setValueTextSize(9f);
        setY.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        setY.setScatterShapeSize(pSize);
        actualData.add(setY);
        calibData.add(getCalibLine("y calib", Color.rgb(0, 0xe0, 0), yCalib, minTime, maxTime));

        ScatterDataSet setZ;
        setZ = new ScatterDataSet(z, "z");
        setZ.setDrawIcons(false);
        setZ.setColor(Color.BLUE);
        setZ.setValueTextSize(9f);
        setZ.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        setZ.setScatterShapeSize(pSize);
        actualData.add(setZ);
        calibData.add(getCalibLine("z calib", Color.rgb(0, 0, 0xe0), zCalib, minTime, maxTime));

        if (marks.size() > 0) {
            ScatterDataSet setMarks;
            setMarks = new ScatterDataSet(marks, "marks");
            setMarks.setDrawIcons(false);
            setMarks.setColor(Color.MAGENTA);
            setMarks.setValueTextSize(9f);
            setMarks.setScatterShape(ScatterChart.ScatterShape.TRIANGLE);
            setMarks.setScatterShapeSize(20f);
            actualData.add(setMarks);
        }

        CombinedData data = new CombinedData();
        data.setData(new LineData(calibData));
        data.setData(new ScatterData(actualData));
        mChart.setData(data);
    }

    private LineDataSet getCalibLine(String label, int color, float value, float minTime, float maxTime) {
        LineDataSet s;
        ArrayList<Entry> vals = new ArrayList<>();
        vals.add(new Entry(minTime, value));
        vals.add(new Entry(maxTime, value));
        s = new LineDataSet(vals, label);
        s.setDrawIcons(false);
        s.setColor(color);
        s.setCircleColor(color);
        s.setLineWidth(2f);
        s.setCircleRadius(3f);
        s.setDrawCircleHole(false);
        s.setValueTextSize(9f);
        return s;
    }

}
