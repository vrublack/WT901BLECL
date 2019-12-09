package com.witsensor.WTBLE901.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.witsensor.WTBLE901.R;


/**
 * Created by 葛文博 on 2017/10/23.
 */
public class DeviceNameDialog extends BDialog {

    @InjectView(R.id.et_putPs)
    EditText et_passWord;

    PsDialogCallBack psDialogCallBack;

    public void setPsDialogCallBack(PsDialogCallBack psDialogCallBack) {
        this.psDialogCallBack = psDialogCallBack;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ps_dialog, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public DeviceNameDialog() {

    }

    public static DeviceNameDialog newIntence() {
        DeviceNameDialog passWordDialog = new DeviceNameDialog();
        return passWordDialog;
    }


    @OnClick(R.id.bt_sure)
    void sure() {
        String str = et_passWord.getText().toString();
        if (psDialogCallBack != null) {
            psDialogCallBack.sure(str);
        }
        dismiss();
    }

    @OnClick(R.id.bt_abolish)
    void abolish() {
        if (psDialogCallBack != null) {
            psDialogCallBack.abolish();
        }
        dismiss();
    }

    public interface PsDialogCallBack {

        void sure(String value);

        void abolish();

    }

}
