package com.example.android.WTBLE901.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.WTBLE901.AngleEvent;
import com.example.android.WTBLE901.LineChartManager;
import com.example.android.WTBLE901.Utils;
import com.example.android.WTBLE901.data.Data;
import com.example.android.WTBLE901.fragment.CompassFragment;
import com.example.android.WTBLE901.fragment.FragmentAdapter;
import com.example.android.WTBLE901.fragment.GraphFragment;
import com.example.android.WTBLE901.view.DeviceNameDialog;
import com.github.mikephil.charting.charts.LineChart;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import wtzn.wtbtble901.R;


public class DeviceControlActivity extends FragmentActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private Button btnAcc, btnGyro, btnAngle, btnMag, btnPressure, btnPort, btnQuater;
    private TextView tvX, tvY, tvZ, tvAll;
    private boolean mConnected = false;
    //新增折线图
    private LineChart lineChart;
    private LineChartManager lineChartManager;
    //角速度CopeBLEData
    private List<Float> wList = new ArrayList<>(); //数据集合
    private List<String> wNames = new ArrayList<>(); //折线名字集合
    private List<Integer> wColour = new ArrayList<>();//折线颜色集合
    //加速度
    private List<Float> aList = new ArrayList<>(); //数据集合
    private List<String> aNames = new ArrayList<>(); //折线名字集合
    private List<Integer> aColour = new ArrayList<>();//折线颜色集合
    //角度
    private List<Float> angleList = new ArrayList<>(); //数据集合
    private List<String> angleNames = new ArrayList<>(); //折线名字集合
    private List<Integer> angleColour = new ArrayList<>();//折线颜色集合
    //磁场
    private List<Float> hList = new ArrayList<>(); //数据集合
    private List<String> hNames = new ArrayList<>(); //折线名字集合
    private List<Integer> hColour = new ArrayList<>();//折线颜色集合
    //气压
    private List<Float> heightList = new ArrayList<>(); //数据集合
    private List<String> heightNames = new ArrayList<>(); //折线名字集合
    private List<Integer> heightColour = new ArrayList<>();//折线颜色集合
    //端口
    private List<Float> dList = new ArrayList<>(); //数据集合
    private List<String> dNames = new ArrayList<>(); //折线名字集合
    private List<Integer> dColour = new ArrayList<>();//折线颜色集合
    //四元数
    private List<Float> qList = new ArrayList<>(); //数据集合
    private List<String> qNames = new ArrayList<>(); //折线名字集合
    private List<Integer> qColour = new ArrayList<>();//折线颜色集合

    private ViewPager mViewPager;

    private LinearLayout mLayout;

    private List<Fragment> listFragment = new ArrayList<>();

    private GraphFragment graphFragment;

    private boolean bMagCali = false;
    private boolean myAcli = false;

    private DrawerLayout draw;

    private boolean isR = true;

    private TextView tvRecord;

    private TextView tvID, tvCell, tvIDName;

    //ffaa274100

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    int DisplayIndex = 0;

    private void SetCurrentTab(View v) {
        btnAcc.setBackgroundColor(0xff33b5e5);
        btnGyro.setBackgroundColor(0xff33b5e5);
        btnAngle.setBackgroundColor(0xff33b5e5);
        btnMag.setBackgroundColor(0xff33b5e5);
        btnPressure.setBackgroundColor(0xff33b5e5);
        btnPort.setBackgroundColor(0xff33b5e5);
        btnQuater.setBackgroundColor(0xff33b5e5);
        ((Button) v).setBackgroundColor(0xff0099cc);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnAcc) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleX)).setText("ax:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("ay:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("az:");
            tvZ.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("|acc|:");
            tvAll.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, aNames, aColour);
                    lineChartManager.setDescription("");
                }
            }, 600);

            DisplayIndex = 0;
            SetCurrentTab(v);
        } else if (i == R.id.btnGyro) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, wNames, wColour);
                    lineChartManager.setDescription("");
                }
            }, 600);
            DisplayIndex = 1;
            ((TextView) findViewById(R.id.tvTittleX)).setText("wx:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("wy:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("wz:");
            tvZ.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("|gyr|:");
            tvAll.setVisibility(View.VISIBLE);
            SetCurrentTab(v);


        } else if (i == R.id.btnAngle) {
            lineChart.clear();
            lineChartManager = new LineChartManager(lineChart, angleNames, angleColour);
            lineChartManager.setDescription("");
            SetCurrentTab(v);
            DisplayIndex = 2;
            //隐藏
            mLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.VISIBLE);
            listFragment.clear();
            listFragment.add(new GraphFragment());
            listFragment.add(new CompassFragment());
            FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager(), listFragment);
            mViewPager.setAdapter(adapter);
            mViewPager.setOnPageChangeListener(this);

        } else if (i == R.id.btnMag) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, hNames, hColour);
                    lineChartManager.setDescription("");
                }
            }, 600);

            ((TextView) findViewById(R.id.tvTittleX)).setText("hx:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("hy:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("hz:");
            tvZ.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("|magn|");
            tvAll.setVisibility(View.VISIBLE);
            SetCurrentTab(v);
            DisplayIndex = 3;


        } else if (i == R.id.btnPressure) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, heightNames, heightColour);
                    lineChartManager.setDescription("");
                }
            }, 600);

            ((TextView) findViewById(R.id.tvTittleX)).setText("pressure:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("altitude:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("");
            tvZ.setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("");
            tvAll.setVisibility(View.INVISIBLE);
            SetCurrentTab(v);
            DisplayIndex = 4;


        } else if (i == R.id.btnPort) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, dNames, dColour);
                    lineChartManager.setDescription("");
                }
            }, 600);

            ((TextView) findViewById(R.id.tvTittleX)).setText("D0:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("D1:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("D2");
            tvZ.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("D3");
            tvAll.setVisibility(View.VISIBLE);
            SetCurrentTab(v);
            DisplayIndex = 5;


        } else if (i == R.id.btnQuater) {
            mViewPager.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
            lineChart.clear();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    lineChartManager = new LineChartManager(lineChart, qNames, qColour);
                    lineChartManager.setDescription("");
                }
            }, 600);

            ((TextView) findViewById(R.id.tvTittleX)).setText("q0:");
            ((TextView) findViewById(R.id.tvTittleY)).setText("q1:");
            ((TextView) findViewById(R.id.tvTittleZ)).setText("q2");
            tvZ.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvTittleAll)).setText("q3");
            tvAll.setVisibility(View.VISIBLE);
            SetCurrentTab(v);
            DisplayIndex = 6;
        }
    }

    private void Rate() {
        iOutputRate = 6;
        new AlertDialog.Builder(this)
                .setTitle(R.string.SelectRate)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"0.1Hz", "0.2Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10Hz", "20Hz", "50Hz"}, 6, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iOutputRate = i;
                    }
                })
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x03, (byte) iOutputRate, (byte) 0x00});
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }


    public void CopeBLEData(byte[] packBuffer) {
        float[] fData = new float[9];
        if (packBuffer != null && packBuffer.length == 20) {
            switch (packBuffer[1]) {
                case 0x61:
                    for (int i = 0; i < 9; i++) {
                        fData[i] = (((short) packBuffer[i * 2 + 3]) << 8) | ((short) packBuffer[i * 2 + 2] & 0xff);
                    }

                    for (int i = 0; i < 3; i++) a[i] = (float) (fData[i] / 32768.0 * 16.0);
                    for (int i = 3; i < 6; i++) w[i - 3] = (float) (fData[i] / 32768.0 * 2000.0);
                    for (int i = 6; i < 9; i++) Angle[i - 6] = (float) (fData[i] / 32768.0 * 180.0);
                    break;
                case 0x71:
                    if (fData[2] != 0x68) {
                        for (int i = 0; i < 8; i++) {
                            fData[i] = (((short) packBuffer[i * 2 + 5]) << 8) | ((short) packBuffer[i * 2 + 4] & 0xff);
                        }
                    }

                    switch (packBuffer[2]) {
                        case 0x3A:
                            for (int i = 0; i < 3; i++) h[i] = fData[i];
                            break;
                        case 0x45:
                            Pressure = ((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff));
                            Height = (((((long) packBuffer[11]) << 24) & 0xff000000) | ((((long) packBuffer[10]) << 16) & 0xff0000) | ((((long) packBuffer[9]) << 8) & 0xff00) | ((((long) packBuffer[8]) & 0xff))) / 100.0f;
                            break;
                        case 0x41:
                            for (int i = 0; i < 4; i++) Port[i] = (float) (fData[i]);
                            break;
                        case 0x51:
                            for (int i = 0; i < 4; i++) q[i] = (float) (fData[i] / 32768.0);
                            break;
                        case 0x40:
                            T = (float) (fData[0] / 100.0);
                            break;
                        case 0x64://电量
                            for (int i = 0; i < 4; i++) {
                                c[i] = (float) (fData[i]);
                            }
                            if (fData[0] < 680) {
                                tvCell.setBackground(getResources().getDrawable(R.drawable.cell1));
                            }
                            if (fData[0] >= 680 && fData[0] < 735) {
                                tvCell.setBackground(getResources().getDrawable(R.drawable.cell2));
                            }

                            if (fData[0] >= 745 && fData[0] < 775) {
                                tvCell.setBackground(getResources().getDrawable(R.drawable.cell3));
                            }

                            if (fData[0] >= 775 && fData[0] < 850) {
                                tvCell.setBackground(getResources().getDrawable(R.drawable.cell4));
                            }

                            if (fData[0] >= 850) {
                                tvCell.setBackground(getResources().getDrawable(R.drawable.cell5));
                            }
                            break;
                        case 0x68:
                            try {
                                String s = new String(packBuffer, "ascii");
                                int end = s.indexOf("WT");
                                if (end == -1) {
                                    tvIDName.setVisibility(View.INVISIBLE);
                                    tvID.setVisibility(View.INVISIBLE);
                                    return;
                                } else {
                                    s = s.substring(4, end);
                                    tvID.setText(s);
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                    }
                    break;
                default:
                    break;
            }



        }
    }

//
//    public static byte[] FloatArrayToByteArray(float[] data) {
//        byte[] Resutl = {};
//        for (int i = 0; i < data.length; i++) {
//            byte[] intToBytes2 = intToBytes2(Float.floatToIntBits(data[i]));
//            byte[] temp = new byte[4];
//            temp[0] = intToBytes2[3];
//            temp[1] = intToBytes2[2];
//            temp[2] = intToBytes2[1];
//            temp[3] = intToBytes2[0];
//            Resutl = concat(Resutl, temp);
//        }
//        return Resutl;
//    }
//
//
//
//    public static byte[] concat(byte[] acc, byte[] b) {
//        byte[] whatIsThis = new byte[acc.length + b.length];
//        System.arraycopy(acc, 0, whatIsThis, 0, acc.length);
//        System.arraycopy(b, 0, whatIsThis, acc.length, b.length);
//        return whatIsThis;
//    }

    public void onClickAccCali(View view) {
        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
    }

    public void onClickAccCaliL(View view) {
        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x05, (byte) 0x00});
    }

    public void onClickAccCaliR(View view) {
        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x06, (byte) 0x00});
    }

    public void onClickReset(View view) {
        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x01, (byte) 0x00});
    }


//    public void onClickMagCali(View view) {
//        Button btnMagCali = ((Button) findViewById(R.id.btnMagCali));
//        if (bMagCali == false) {
//            mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00});
//            btnMagCali.setText(R.string.Finish);
//            bMagCali = true;
//        } else {
//            mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
//            btnMagCali.setText(R.string.MagCali);
//            bMagCali = false;
//        }
//    }

//    public void onClickSave(View view) {
//        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//    }

    int iPortMode = 0;
    int iOutputRate = 0;

    private void SetPortMode(final int iPortIndex) {
        iPortMode = 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.SelectPortMode)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"AIn", "DIn", "DOutH", "DOutL"}, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iPortMode = i;
                    }
                })
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x0E + iPortIndex), (byte) iPortMode, (byte) 0x00});
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }


    private boolean once = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.gatt_services_characteristics);
        initDraw();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // viewpager  linearlayout
        mViewPager = (ViewPager) findViewById(R.id.mViewPager);
        mLayout = (LinearLayout) findViewById(R.id.mLayout);

//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        btnAcc = (Button) findViewById(R.id.btnAcc);
        btnAcc.setOnClickListener(this);
        btnGyro = (Button) findViewById(R.id.btnGyro);
        btnGyro.setOnClickListener(this);
        btnAngle = (Button) findViewById(R.id.btnAngle);
        btnAngle.setOnClickListener(this);
        btnMag = (Button) findViewById(R.id.btnMag);
        btnMag.setOnClickListener(this);
        btnPressure = (Button) findViewById(R.id.btnPressure);
        btnPressure.setOnClickListener(this);
        btnPort = (Button) findViewById(R.id.btnPort);
        btnPort.setOnClickListener(this);
        btnQuater = (Button) findViewById(R.id.btnQuater);
        btnQuater.setOnClickListener(this);
        TextView textView = (TextView) findViewById(R.id.tv_set);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                draw.openDrawer(Gravity.LEFT);
            }
        });
        lineChart = (LineChart) findViewById(R.id.lineChart);
        initChart();
        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);
        tvZ = (TextView) findViewById(R.id.tvZ);
        tvAll = (TextView) findViewById(R.id.tvAll);
        tvRecord = (TextView) findViewById(R.id.tv_record);
        tvCell = (TextView) findViewById(R.id.tv_cell);
        tvID = (TextView) findViewById(R.id.tv_id);
        tvIDName = (TextView) findViewById(R.id.tv_nameId);

        onClick(findViewById(R.id.btnAngle));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (mConnected) {
                            handler.sendEmptyMessageAtTime(0, 5000);
                            switch (DisplayIndex) {
                                case 2://angle
                                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x40, (byte) 0x00});
                                    break;
                                case 3://Mag
                                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x3A, (byte) 0x00});
                                    break;
                                case 4://pressure
                                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x45, (byte) 0x00});
                                    break;
                                case 5://port
                                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x41, (byte) 0x00});
                                    break;
                                case 6://Quater
                                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x51, (byte) 0x00});
                                    break;
                            }
//
                        }
                        Thread.sleep(240);
                    } catch (Exception err) {
                    }
                }
            }
        });
        thread.start();


        tvID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceNameDialog dialog = DeviceNameDialog.newIntence();
                dialog.setPsDialogCallBack(new DeviceNameDialog.PsDialogCallBack() {
                    @Override
                    public void sure(String value) {
                        short id = Short.valueOf(value);
                        byte l = (byte) (id & 0xff);
                        byte h = (byte) (id << 8 & 0xff);
                        byte[] bb = new byte[]{(byte) 0xff, (byte) 0xaa, 0x69, l, h};
                        mBluetoothLeService.writeByes(bb);
                        mBluetoothLeService.writeByes(mDeviceID);
                    }

                    @Override
                    public void abolish() {

                    }
                });
                dialog.show(getSupportFragmentManager());
            }
        });

        //记录
        tvRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mBluetoothLeService.isRecording()) {
                    tvRecord.setText(getString(R.string.menu_stop));
                } else {
                    tvRecord.setText(getString(R.string.Record));
                    new AlertDialog.Builder(DeviceControlActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage(getString(R.string.data_record) + "/mnt/sdcard/Record.txt\n" + getString(R.string.open_file))
                            .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    try {
                                        File myFile = new File("/mnt/sdcard/Record.txt");
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.fromFile(myFile));
                                        startActivity(intent);
                                    } catch (Exception err) {
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.Cancel), null)
                            .show();
                }

                mBluetoothLeService.toggleRecording();
            }
        });
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if (mConnected) {
                  //  try {
                        mBluetoothLeService.writeByes(mDeviceID);
                        mBluetoothLeService.writeByes(cell);
                 //   }
                  //  catch (Exception err){}
                }
            }
        }
    };


    private BluetoothLeService.UICallback callback = new BluetoothLeService.UICallback() {
        @Override
        public void handleBLEData(final Data data) {
            switch (DisplayIndex) {
                case 0://acc
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            aList.add(data.getAcc()[0]);
                            aList.add(data.getAcc()[1]);
                            aList.add(data.getAcc()[2]);
                            lineChartManager.addEntry(aList);
                            aList.clear();
                            tvX.setText(String.format("%.2fg", data.getAcc()[0]));
                            tvY.setText(String.format("%.2fg", data.getAcc()[1]));
                            tvZ.setText(String.format("%.2fg", data.getAcc()[2]));
                            tvAll.setText(String.format("%.2fg", Utils.Norm(data.getAcc())));
                        }
                    }, 100);
                    break;
                case 1://gyr
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            wList.add(data.getGyr()[0]);
                            wList.add(data.getGyr()[1]);
                            wList.add(data.getGyr()[2]);
                            lineChartManager.addEntry(wList);
                            wList.clear();
                            tvX.setText(String.format("%.2f°/s", data.getGyr()[0]));
                            tvY.setText(String.format("%.2f°/s", data.getGyr()[1]));
                            tvZ.setText(String.format("%.2f°/s", data.getGyr()[2]));
                            tvAll.setText(String.format("%.2f°/s", Utils.Norm(data.getGyr())));
                        }
                    }, 100);

                    break;
                case 2://angle
                    Bundle bundle = new Bundle();
                    bundle.putFloat("x", data.getAngle()[0]);
                    bundle.putFloat("y", data.getAngle()[1]);
                    bundle.putFloat("z", data.getAngle()[2]);
                    EventBus.getDefault().post(new AngleEvent(bundle));
                    break;
                case 3://magn

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hList.add(data.getMagn()[0]);
                            hList.add(data.getMagn()[1]);
                            hList.add(data.getMagn()[2]);
                            lineChartManager.addEntry(hList);
                            hList.clear();
                            tvX.setText(String.format("%.0f", data.getMagn()[0]));
                            tvY.setText(String.format("%.0f", data.getMagn()[1]));
                            tvZ.setText(String.format("%.0f", data.getMagn()[2]));
                            tvAll.setText(String.format("%.0f", Utils.Norm(data.getMagn())));
                        }
                    }, 100);

                    break;
                case 4://pressure
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            heightList.add(data.getAltitude());
                            lineChartManager.addEntry(heightList);
                            heightList.clear();
                            tvX.setText(String.format("%.2fPa", data.getPressure()));
                            tvY.setText(String.format("%.2fm", data.getAltitude()));
                            Log.e("DeviceControlActivity", "pressure=" + tvX.getText().toString() + "Heigh=" + tvY.getText().toString());
                        }
                    }, 100);

                    break;
                case 5://port
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dList.add(data.getPort()[0]);
                            dList.add(data.getPort()[1]);
                            dList.add(data.getPort()[2]);
                            dList.add(data.getPort()[3]);
                            lineChartManager.addEntry(dList);
                            dList.clear();
                            tvX.setText(String.format("%.0f", data.getPort()[0]));
                            tvY.setText(String.format("%.0f", data.getPort()[1]));
                            tvZ.setText(String.format("%.0f", data.getPort()[2]));
                            tvAll.setText(String.format("%.0f", data.getPort()[3]));
                        }
                    }, 100);

                    break;
                case 6://Quater
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            qList.add(data.getQuaternion()[0]);
                            qList.add(data.getQuaternion()[1]);
                            qList.add(data.getQuaternion()[2]);
                            qList.add(data.getQuaternion()[3]);
                            lineChartManager.addEntry(qList);
                            qList.clear();
                            tvX.setText(String.format("%.3f", data.getQuaternion()[0]));
                            tvY.setText(String.format("%.3f", data.getQuaternion()[1]));
                            tvZ.setText(String.format("%.3f", data.getQuaternion()[2]));
                            tvAll.setText(String.format("%.3f", data.getQuaternion()[3]));
                        }
                    }, 100);
                    break;
            }
        }

        @Override
        public void onConnected(String deviceName) {
            invalidateOptionsMenu();
        }

        @Override
        public void onDisconnected() {
            invalidateOptionsMenu();
        }
    };


    private void initDraw() {
        draw = (DrawerLayout) findViewById(R.id.drawerLayout);
        NavigationView nv = (NavigationView) findViewById(R.id.nv_layout);
        final TextView acli = (TextView) findViewById(R.id.tv_acli);
        TextView acli_l = (TextView) findViewById(R.id.tv_cali_l);
        TextView acli_r = (TextView) findViewById(R.id.tv_cali_r);
        final TextView magacli = (TextView) findViewById(R.id.tv_magcali);
        TextView d0 = (TextView) findViewById(R.id.tv_d0);
        TextView d1 = (TextView) findViewById(R.id.tv_d1);
        TextView d2 = (TextView) findViewById(R.id.tv_d2);
        TextView d3 = (TextView) findViewById(R.id.tv_d3);
        TextView rete = (TextView) findViewById(R.id.tv_rate);
        TextView resume = (TextView) findViewById(R.id.tv_resume);
        TextView rename = (TextView) findViewById(R.id.tv_rename);
        //加计校准
        acli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myAcli == false) {
                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
                    acli.setText(R.string.Finish);
                    myAcli = true;
                } else {
                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
                    acli.setText(R.string.AccCali);
                    myAcli = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
                        }
                    }, 20);
                }
            }
        });
        //加计校准L
        acli_l.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x05, (byte) 0x00});
            }
        });
        //加计校准R
        acli_r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x06, (byte) 0x00});
            }
        });

        //磁场校准
        magacli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bMagCali == false) {
                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00});
                    magacli.setText(R.string.Finish);
                    bMagCali = true;
                } else {
                    mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
                    magacli.setText(R.string.MagCali);
                    bMagCali = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
                            draw.closeDrawer(Gravity.LEFT);
                        }
                    }, 100);
                }
            }
        });

        //D0
        d0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetPortMode(0);
                draw.closeDrawer(Gravity.LEFT);
            }
        });

        //D1
        d1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetPortMode(1);
                draw.closeDrawer(Gravity.LEFT);
            }
        });

        //D2
        d2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetPortMode(2);
                draw.closeDrawer(Gravity.LEFT);
            }
        });

        //D3
        d3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetPortMode(3);
                draw.closeDrawer(Gravity.LEFT);
            }
        });

        //速率
        rete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Rate();
                draw.closeDrawer(Gravity.LEFT);
            }
        });
        //恢复
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x01, (byte) 0x00});
            }
        });
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText editText = new EditText(DeviceControlActivity.this);
                AlertDialog.Builder inputDialog =
                        new AlertDialog.Builder(DeviceControlActivity.this);
                inputDialog.setTitle(R.string.EnterNewName).setView(editText);
                inputDialog.setPositiveButton(R.string.OK,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String value= editText.getText().toString();
                                if (value.length()>10) value = value.substring(0,9);
                                if (value != null && !value.equals("")) {
                                    String name = "WTWT" + value + "\r\n";
                                    Log.e("------", "WT====" + name);
                                    mBluetoothLeService.writeByes(name.getBytes());
                                    Toast.makeText(DeviceControlActivity.this, R.string.Reset, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(DeviceControlActivity.this, R.string.EnterName, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
                inputDialog.setNegativeButton(R.string.Cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               Toast.makeText(DeviceControlActivity.this,
                                       R.string.Cancel,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }
        });
    }

    private void initChart() {
        // 初始化所有chart的标注和线
        aNames.add("ax");
        aNames.add("ay");
        aNames.add("az");
        aColour.add(Color.RED);
        aColour.add(Color.GREEN);
        aColour.add(Color.BLUE);

        wNames.add("wx");
        wNames.add("wy");
        wNames.add("wz");
        wColour.add(Color.RED);
        wColour.add(Color.GREEN);
        wColour.add(Color.BLUE);

        angleNames.add("AngleX");
        angleNames.add("AngleY");
        angleNames.add("AngleZ");
        angleColour.add(Color.RED);
        angleColour.add(Color.GREEN);
        angleColour.add(Color.BLUE);

        hNames.add("hx");
        hNames.add("hy");
        hNames.add("hz");
        hColour.add(Color.RED);
        hColour.add(Color.GREEN);
        hColour.add(Color.BLUE);

        heightColour.add(Color.RED);
        heightNames.add("altitude");

        dNames.add("D0");
        dNames.add("D1");
        dNames.add("D2");
        dNames.add("D3");
        dColour.add(Color.RED);
        dColour.add(Color.GREEN);
        dColour.add(Color.BLUE);
        dColour.add(Color.GRAY);

        qNames.add("q0");
        qNames.add("q1");
        qNames.add("q2");
        qNames.add("q3");
        qColour.add(Color.RED);
        qColour.add(Color.GREEN);
        qColour.add(Color.BLUE);
        qColour.add(Color.DKGRAY);


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_connect) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            return true;
        } else if (i == R.id.menu_disconnect) {
            mBluetoothLeService.disconnect();
            if (mBluetoothLeService != null) {
                unbindService(mServiceConnection);
                mBluetoothLeService = null;
            }
            return true;
        } else if (i == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


}
