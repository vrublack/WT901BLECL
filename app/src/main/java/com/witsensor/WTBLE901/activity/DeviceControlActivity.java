package com.witsensor.WTBLE901.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.witsensor.WTBLE901.AngleEvent;
import com.witsensor.WTBLE901.LineChartManager;
import com.witsensor.WTBLE901.Utils;
import com.witsensor.WTBLE901.data.Data;
import com.witsensor.WTBLE901.fragment.CompassFragment;
import com.witsensor.WTBLE901.fragment.FragmentAdapter;
import com.witsensor.WTBLE901.fragment.GraphFragment;
import com.witsensor.WTBLE901.view.ChartActivity;
import com.witsensor.WTBLE901.view.DeviceNameDialog;
import com.github.mikephil.charting.charts.LineChart;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import com.witsensor.WTBLE901.R;


public class DeviceControlActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ViewPager.OnPageChangeListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final int REQUEST_CONNECT_DEVICE = 1;

    private String mToConnectTo;
    private BluetoothLeService mService;
    private Button btnAcc, btnGyro, btnAngle, btnMag, btnPressure, btnPort, btnQuater;
    private TextView tvX, tvY, tvZ, tvAll;
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

    int DisplayIndex = 0;
    private DrawerLayout drawerLayout;

    private ViewPager mViewPager;

    private LinearLayout mLayout;

    private List<Fragment> listFragment = new ArrayList<>();

    private boolean bMagCali = false;
    private boolean myAcli = false;

    private DrawerLayout draw;

    private TextView tvID, tvCell, tvIDName;

    private Thread mRefreshSensor;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = ((BluetoothLeService.LocalBinder) service).getService();

            if (mToConnectTo != null) {
                if (mService.connect(mToConnectTo)) {
                    mToConnectTo = null;
                } else {
                    Toast.makeText(DeviceControlActivity.this, getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
                }
            }

            mService.setUICallback(mCallback);

            Log.d(DeviceControlActivity.class.getCanonicalName(), "onServiceConnected");

            // update UI now
            if (mService.isConnected()) {
                mNavView.getMenu().findItem(R.id.action_connect).setTitle(R.string.menu_disconnect);
                invalidateOptionsMenu();
            } else {
                mNavView.getMenu().findItem(R.id.action_connect).setTitle(R.string.menu_connect);
                invalidateOptionsMenu();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };
    private NavigationView mNavView;

    private void SetCurrentTab(View v) {
        btnAcc.setAlpha(0.6f);
        btnGyro.setAlpha(0.6f);
        btnAngle.setAlpha(0.6f);
        btnMag.setAlpha(0.6f);
        btnPressure.setAlpha(0.6f);
        btnPort.setAlpha(0.6f);
        btnQuater.setAlpha(0.6f);
        ((Button) v).setAlpha(1.0f);
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
        SharedPreferences mySharedPreferences= getSharedPreferences("Output", Activity.MODE_PRIVATE);
        mOutputRate = mySharedPreferences.getInt("Rate", 6);
        new AlertDialog.Builder(this)
                .setTitle(R.string.SelectRate)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"0.1Hz", "0.2Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10Hz", "20Hz", "50Hz"}, mOutputRate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mOutputRate = i;
                    }
                })
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mService.setRate(mOutputRate);
                        SharedPreferences mySharedPreferences= getSharedPreferences("Output",Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = mySharedPreferences.edit();
                        editor.putInt("Rate", mOutputRate);
                        editor.commit();
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
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
        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
    }

    public void onClickAccCaliL(View view) {
        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x05, (byte) 0x00});
    }

    public void onClickAccCaliR(View view) {
        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x06, (byte) 0x00});
    }

    public void onClickReset(View view) {
        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x01, (byte) 0x00});
    }


//    public void onClickMagCali(View view) {
//        Button btnMagCali = ((Button) findViewById(R.id.btnMagCali));
//        if (bMagCali == false) {
//            mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00});
//            btnMagCali.setText(R.string.Finish);
//            bMagCali = true;
//        } else {
//            mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
//            btnMagCali.setText(R.string.MagCali);
//            bMagCali = false;
//        }
//    }

//    public void onClickSave(View view) {
//        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
//    }

    int iPortMode = 0;
    int mOutputRate = 0;

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
                        mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x0E + iPortIndex), (byte) iPortMode, (byte) 0x00});
                    }
                })
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }


    private boolean once = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        configureNavigationDrawer();
        configureToolbar();

        // viewpager  linearlayout
        mViewPager = (ViewPager) findViewById(R.id.mViewPager);
        mLayout = (LinearLayout) findViewById(R.id.mLayout);

//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
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

        lineChart = (LineChart) findViewById(R.id.lineChart);
        initChart();
        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);
        tvZ = (TextView) findViewById(R.id.tvZ);
        tvAll = (TextView) findViewById(R.id.tvAll);
        tvCell = (TextView) findViewById(R.id.tv_cell);
        tvID = (TextView) findViewById(R.id.tv_id);
        tvIDName = (TextView) findViewById(R.id.tv_nameId);

        onClick(findViewById(R.id.btnAngle));


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
                        mService.writeByes(bb);
                        mService.writeByes(BluetoothLeService.mDeviceID);
                    }

                    @Override
                    public void abolish() {

                    }
                });
                dialog.show(getSupportFragmentManager());
            }
        });

        if (!BluetoothLeService.isRunning) {
            if (getDefaultDevice() == null) {
                try {
                    Intent serverIntent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } catch (Exception err) {
                }
            } else {
                mToConnectTo = getDefaultDevice();
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:// When DeviceListActivity returns with a device to connect
                if (intent != null) {
                    String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
                    mToConnectTo = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                    setDefaultDevice(mToConnectTo);
                    if (resultCode == Activity.RESULT_OK && mService != null) {
                        if (mService.connect(mToConnectTo)) {
                            mToConnectTo = null;
                        } else {
                            Toast.makeText(DeviceControlActivity.this, getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if (mService != null && mService.isConnected()) {
                    mService.writeByes(BluetoothLeService.mDeviceID);
                    mService.writeByes(BluetoothLeService.cell);
                }
            }
        }
    };


    private BluetoothLeService.UICallback mCallback = new BluetoothLeService.UICallback() {
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

            if (data.getBattery() < 680) {
                tvCell.setBackground(getResources().getDrawable(R.drawable.cell1));
            }
            if (data.getBattery() >= 680 && data.getBattery() < 735) {
                tvCell.setBackground(getResources().getDrawable(R.drawable.cell2));
            }

            if (data.getBattery() >= 745 && data.getBattery() < 775) {
                tvCell.setBackground(getResources().getDrawable(R.drawable.cell3));
            }

            if (data.getBattery() >= 775 && data.getBattery() < 850) {
                tvCell.setBackground(getResources().getDrawable(R.drawable.cell4));
            }

            if (data.getBattery() >= 850) {
                tvCell.setBackground(getResources().getDrawable(R.drawable.cell5));
            }

            if (data.getDeviceName() != null) {
                if (data.getDeviceName().length() == 0) {
                    tvIDName.setVisibility(View.INVISIBLE);
                    tvID.setVisibility(View.INVISIBLE);
                } else {
                    tvID.setText(data.getDeviceName());
                }
            }
        }

        @Override
        public void onConnected(String deviceName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DeviceControlActivity.this.findViewById(R.id.tv_connect).setVisibility(View.GONE);
                    DeviceControlActivity.this.findViewById(R.id.action_connect).setVisibility(View.VISIBLE);
                    Toast.makeText(DeviceControlActivity.this, R.string.connected, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.spinner).setVisibility(View.GONE);
                    invalidateOptionsMenu();
                }
            });
        }

        @Override
        public void onDisconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DeviceControlActivity.this.findViewById(R.id.tv_connect).setVisibility(View.VISIBLE);
                    DeviceControlActivity.this.findViewById(R.id.action_connect).setVisibility(View.GONE);
                    Toast.makeText(DeviceControlActivity.this, R.string.disconnected, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.spinner).setVisibility(View.GONE);
                    invalidateOptionsMenu();
                }
            });
        }
    };

    private void configureToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setHomeAsUpIndicator(R.drawable.ic_drawer);
        actionbar.setDisplayHomeAsUpEnabled(true);
    }

    private void configureNavigationDrawer() {
        draw = (DrawerLayout) findViewById(R.id.drawerLayout);
        mNavView = (NavigationView) findViewById(R.id.nv_layout);
        mNavView.setNavigationItemSelectedListener(this);
    }

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
        TextView disconnect = (TextView) findViewById(R.id.action_connect);
        //加计校准
        acli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myAcli == false) {
                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
                    acli.setText(R.string.Finish);
                    myAcli = true;
                } else {
                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
                    acli.setText(R.string.AccCali);
                    myAcli = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
                        }
                    }, 20);
                }
            }
        });
        //加计校准L
        acli_l.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x05, (byte) 0x00});
            }
        });
        //加计校准R
        acli_r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x06, (byte) 0x00});
            }
        });

        //磁场校准
        magacli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bMagCali == false) {
                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00});
                    magacli.setText(R.string.Finish);
                    bMagCali = true;
                } else {
                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
                    magacli.setText(R.string.MagCali);
                    bMagCali = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
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
                mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x01, (byte) 0x00});
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
                                    mService.writeByes(name.getBytes());
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

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mService != null) {
                    mService.disconnect();
                    findViewById(R.id.spinner).setVisibility(View.VISIBLE);
                    view.setVisibility(View.GONE);
                }
            }
        });

        findViewById(R.id.tv_connect_other).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mService != null)
                    mService.disconnect();
                setDefaultDevice(null);
                Intent serverIntent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });

        findViewById(R.id.tv_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String defaultDevice = getDefaultDevice();
                if (mService != null && defaultDevice != null) {
                    if (mService.connect(defaultDevice)) {
                        findViewById(R.id.spinner).setVisibility(View.VISIBLE);
                        view.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(DeviceControlActivity.this, getString(R.string.connect_failed), Toast.LENGTH_SHORT).show();
                    }
                }
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

    @Nullable
    private String getDefaultDevice() {
        return getSharedPreferences("Device", Activity.MODE_PRIVATE).getString("defaultDevice", null);
    }

    private void setDefaultDevice(String address) {
        SharedPreferences mySharedPreferences = getSharedPreferences("Device", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putString("defaultDevice", address);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!BluetoothLeService.isRunning) {
            mToConnectTo = getDefaultDevice();
            startSensorService();
        }

        // maybe the service was running in the background but the activity was destroyed
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

        mRefreshSensor = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (mService != null && mService.isConnected()) {
                            handler.sendEmptyMessageAtTime(0, 5000);
                            switch (DisplayIndex) {
                                case 2://Angle
                                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x40, (byte) 0x00});
                                    break;
                                case 3://Mag
                                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x3A, (byte) 0x00});
                                    break;
                                case 4://Pressure
                                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x45, (byte) 0x00});
                                    break;
                                case 5://Port
                                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x41, (byte) 0x00});
                                    break;
                                case 6://Quater
                                    mService.writeByes(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x51, (byte) 0x00});
                                    break;
                            }
                        }
                        Thread.sleep(240);
                    }
                } catch (InterruptedException err) {
                }
            }
        });
        mRefreshSensor.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.act_device_control, menu);
        return true;
    }


    private void startSensorService() {
        Log.d(TAG, "Starting service");
        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(DeviceControlActivity.class.getCanonicalName(), "onStop");
        if (mService != null) {
            mService.setUICallback(null);
            if (!mService.isRecording()) {
                // shut service down if not recording because we don't need it
                stopService(new Intent(this, BluetoothLeService.class));
            }
            unbindService(mServiceConnection);
            mService = null;
        }

        mRefreshSensor.interrupt();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_connect) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            return true;
        } else if (i == R.id.menu_disconnect) {
            mService.disconnect();
            if (mService != null) {
                unbindService(mServiceConnection);
                mService = null;
            }
            return true;
        } else if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == R.id.action_record) {
            if (mService == null || !mService.isConnected())
                return true;
            if (!mService.isRecording()) {
            } else {
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(getString(R.string.hint))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(getString(R.string.data_record) + mService.getPath().toString() + "\n" + getString(R.string.open_file))
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider",
                                        mService.getPath());
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(uri);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(getString(R.string.Cancel), null)
                        .show();
            }

            mService.toggleRecording();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem recordingItem = menu.findItem(R.id.action_record);

        if (mService.isConnected()) {
            recordingItem.setEnabled(true);
            recordingItem.getIcon().setAlpha(255);
        } else {
            // disabled
            recordingItem.setEnabled(false);
            recordingItem.getIcon().setAlpha(130);
        }

        if (mService.isRecording()) {
            recordingItem.setIcon(R.drawable.ic_stop);
            recordingItem.setTitle(R.string.menu_stop);
        } else {
            recordingItem.setIcon(R.drawable.ic_record);
            recordingItem.setTitle(R.string.Record);
        }

        return true;
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


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment f = null;
        int itemId = menuItem.getItemId();

        if (itemId == R.id.action_mark) {
            if (mService != null)
                mService.addMark();
        } else if (itemId == R.id.action_chart) {
            startActivity(new Intent(DeviceControlActivity.this, ChartActivity.class));
        }

        drawerLayout.closeDrawers();
        return true;
    }
}
