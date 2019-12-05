package com.example.android.WTBLE901.fragment;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.WTBLE901.AngleEvent;
import com.example.android.WTBLE901.CompassView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import wtzn.wtbtble901.R;

/**
 * Created by 葛文博 on 2017/10/25.
 */
public class CompassFragment extends BaseFragment {


    private CompassView compassView;

    private TextView tvX, tvY, tvZ;

    private ImageView imageView;


    private List<Float> list = new ArrayList<>();

    float startAngle = 0.0f;

    @Override
    public int getContentViewId() {
        return R.layout.frg_compass;
    }

    @Override
    protected void initAllMembersView(Bundle savedInstanceState) {
        compassView = (CompassView) mRootView.findViewById(R.id.myCpv);
        tvX = (TextView) mRootView.findViewById(R.id.tv_x);
        tvY = (TextView) mRootView.findViewById(R.id.tv_y);
        tvZ = (TextView) mRootView.findViewById(R.id.tv_z);
        imageView = (ImageView) mRootView.findViewById(R.id.iv_Cp);
    }

    private void initAnimation(Float sf, final Float ef) {
        final ObjectAnimator mCircleAnimator = ObjectAnimator.ofFloat(imageView, "rotation", sf, ef);
        mCircleAnimator.setDuration(10);
        mCircleAnimator.setInterpolator(new LinearInterpolator());
        mCircleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mCircleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                startAngle = ef;
            }
        });
        mCircleAnimator.start();
    }


    private void upData(Bundle b) {
        tvX.setText("X:" + String.format("%.2f°", b.getFloat("x")));
        tvY.setText("Y:" + String.format("%.2f°", b.getFloat("y")));
        tvZ.setText("Z:" + String.format("%.2f°", b.getFloat("z")));
        float endAngle = b.getFloat("z");
        initAnimation(startAngle, endAngle);
        int xAngle = (int) b.getFloat("x");
        int yAngle = (int) b.getFloat("y");
        if (xAngle == 0) {
            xAngle = 310;
        } else {
            xAngle = 310 + (xAngle * 8);
        }
        if (yAngle == 0) {
            yAngle = 310;
        } else if (yAngle > 0) {
            yAngle = 310 - (yAngle * 8);
        } else {
            yAngle = 310 + (yAngle * -8);
        }
        compassView.moveCenter(xAngle, yAngle);
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
