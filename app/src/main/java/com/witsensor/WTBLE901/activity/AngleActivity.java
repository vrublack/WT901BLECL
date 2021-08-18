package com.witsensor.WTBLE901.activity;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.witsensor.WTBLE901.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by 葛文博 on 2017/10/25.
 */
public class AngleActivity<Fragment> extends FragmentActivity implements ViewPager.OnPageChangeListener {

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
