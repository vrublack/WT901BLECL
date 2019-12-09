package com.witsensor.WTBLE901.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.witsensor.WTBLE901.AngleEvent;
import com.witsensor.WTBLE901.LineChartManager;
import com.github.mikephil.charting.charts.LineChart;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import wtzn.wtbtble901.R;


/**
 * Created by 葛文博 on 2017/10/25.
 */
public class GraphFragment extends BaseFragment {

    //新增折线图
    private LineChart lineChart;
    private static LineChartManager lineChartManager;

    //角度
    private List<Float> angleList = new ArrayList<>(); //数据集合
    private List<String> angleNames = new ArrayList<>(); //折线名字集合
    private List<Integer> angleColour = new ArrayList<>();//折线颜色集合

    private TextView tvX;
    private TextView tvY;
    private TextView tvZ;

    private float[] angle = new float[]{0, 0, 0};


    @Override
    public int getContentViewId() {
        return R.layout.frg_graph;
    }

    @Override
    protected void initAllMembersView(Bundle savedInstanceState) {
        lineChart = (LineChart) mRootView.findViewById(R.id.lineChart);
        tvX = (TextView) mRootView.findViewById(R.id.tvX);
        tvY = (TextView) mRootView.findViewById(R.id.tvY);
        tvZ = (TextView) mRootView.findViewById(R.id.tvZ);
        lineChart.clear();
        angleNames.clear();
        angleColour.clear();
        angleNames.add("AngleX");
        angleNames.add("AngleY");
        angleNames.add("AngleZ");
        angleColour.add(Color.RED);
        angleColour.add(Color.GREEN);
        angleColour.add(Color.BLUE);
        lineChartManager = new LineChartManager(lineChart, angleNames, angleColour);
        lineChartManager.setDescription("");
    }


    private void upData(Bundle b) {
        tvX.setText(String.format("%.2f°", b.getFloat("x")));
        tvY.setText(String.format("%.2f°", b.getFloat("y")));
        tvZ.setText(String.format("%.2f°", b.getFloat("z")));
        angleList.add(b.getFloat("x"));
        angleList.add(b.getFloat("y"));
        angleList.add(b.getFloat("z"));
        lineChartManager.addEntry(angleList);
        angleList.clear();

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            EventBus.getDefault().register(this);
        } else {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void AngleEvent(AngleEvent event) {
        upData((Bundle) event.getData());
    }
}
