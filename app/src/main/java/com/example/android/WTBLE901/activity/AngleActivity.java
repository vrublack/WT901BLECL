package com.example.android.WTBLE901.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import wtzn.wtbtble901.R;


/**
 * Created by 葛文博 on 2017/10/25.
 */
public class AngleActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private List<Fragment> list = new ArrayList<>();

    private ViewPager mViewPager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.act_angle);
        mViewPager = (ViewPager) findViewById(R.id.mViewPager);
        init();
    }

    private void init() {
//        list.add(new GraphFragment());
//        list.add(new CompassFragment());
//        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager(), list);
//        mViewPager.setAdapter(adapter);
//        mViewPager.setOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position == 0) {
            Toast.makeText(this, "曲线图", Toast.LENGTH_SHORT).show();
        } else if (position == 1) {
            Toast.makeText(this, "指南针", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }



}
