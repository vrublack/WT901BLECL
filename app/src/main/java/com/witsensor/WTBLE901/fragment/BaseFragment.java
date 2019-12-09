package com.witsensor.WTBLE901.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.lang.reflect.Field;


/**
 * ${GWB}
 * 2017/5/2.
 */

public abstract class BaseFragment extends Fragment {

    public FragmentManager FRG_MANAGER;// 掉了static 否则到其他的Activity就会把当前Ac的fragmentmanager替换掉

    protected View mRootView;

    private boolean isDebug;

    private String APP_NAME;

    public abstract int getContentViewId();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getContentViewId(), container, false);
        FRG_MANAGER = getActivity().getSupportFragmentManager();
        initAllMembersView(savedInstanceState);
        return mRootView;
    }


    protected abstract void initAllMembersView(Bundle savedInstanceState);


    @Override
    public void onDetach() {// 解决java.lang.IllegalStateException: Activity has been destroyed的bug
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [日志输出]
     *
     * @param msg
     */
    protected void $Log(String msg) {
        if (isDebug) {
            Log.d(APP_NAME, msg);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
