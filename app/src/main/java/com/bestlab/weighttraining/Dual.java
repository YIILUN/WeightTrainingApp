package com.bestlab.weighttraining;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dual extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int REQUEST_ENABLE_BT = 2;
    private static int Broadcast_ReceiverFlag = 0;
    LocationManager locationManager;
    //BufferedWriter bw;
    private int mState = UART_PROFILE_DISCONNECTED;
    private BLEService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private boolean mScanning;

    //BLE
    String TAG = "brocast";

    //identity
    int height = 0,weight = 0,age = 0;
    String name = null,gender = null;
    int heightGuest = 0,weightGuest = 0,ageGuest = 0;
    String nameGuest = null,genderGuest = null;

    //peak detection
    int TofData;
    int preSlope,nowSlope,preTofData,peakFlag;
    int times,tempTimes = 0;
    int countGroup = 1;
    double distanceIn,distanceOut;

    //Timer
    boolean startFlag = false;
    boolean pauseFlag = false;
    int countData = 0,countForRestTime;
    int pauseTimeStart = 0,totalPauseTime = 0,totalTime = 0;
    int timerIn = 0,timerOut = 0,timerTemp = 0;
    int restTofData = 0, restTimeStart = 0,restTimeCount = 0;

    //Power
    double powerIn = 0,powerOut = 0;
    DecimalFormat df = new DecimalFormat("###0.00");

    //1RM
    double[] chestRM = {1,1.035,1.08,1.115,1.15,1.18,1.22,1.255,1.29,1.325};
    double[] legRM = {1,1.0475,1.13,1.1575,1.2,1.242,1.326,1.368,1.41};
    double RM = 0;
    boolean RMflag = true;

    //frequency
    String frequencyString;
    int frequency = 100;
    int countForFrequency = 0;

    //initial weight setting
    boolean initialWeightFlag = false;
    int [] saveData = new int [500];
    int count = 0;
    BufferedWriter bw;
    String writeWeight;

    int number = 0;
    double[] average = new double [50];
    double[] deviation = new double [50];
    double tempAverage;
    double tempDeviation;

    //get weight
    boolean getWeightFlag = false;

    //SQL
    public SQLdata DH = null;
    public SQLiteDatabase db;
    int sum_id;

    //Tempo
    int countSound = 0;

    //soundpool
    private SoundPool soundPool;
    private int soundup,sounddown,soundtick,soundtock;

    GlobalVariable gv;
    String pose;

    Toast mToast = null;
    TextView txvDeviceName,txvHost,txvGuest,txvConnectState,txvWeightStack,txvExercise;
    Button btnScan,btnEdit,btnStartExercise,btnMachine,btnInitial,btnTempo,btnRM;
    EditText editInSec,editOutSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        //藍芽
        final BluetoothManager bluetoothManager =(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();

        final GlobalVariable gv = (GlobalVariable)getApplicationContext();

        //textView
        txvDeviceName = findViewById(R.id.txvDeviceName);
        txvHost = findViewById(R.id.txvHost);
        txvGuest = findViewById(R.id.txvGuest);
        txvConnectState = findViewById(R.id.txvConnectState);
        txvWeightStack = findViewById(R.id.txvWeightStack);
        txvExercise = findViewById(R.id.txvExercise);

        //button
        btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //到ScanQRcode做掃描藍芽mac
                Intent intentScanQRcode = new Intent();
                intentScanQRcode.setClass(Dual.this, ScanQRcode.class);
                startActivityForResult(intentScanQRcode,REQUEST_CODE);
            }
        });

        btnMachine = findViewById(R.id.btnMachine);
        btnMachine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnInitial.setText("開始掃描");
                //到Initial Machine做設定重量初始化
                Intent intentInitialMachine = new Intent();
                intentInitialMachine.setClass(Dual.this, InitialMachine.class);
                startActivity(intentInitialMachine);
            }
        });


        btnInitial = findViewById(R.id.btnInitial);
        btnInitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txvConnectState.getText() == "已連線") {
                    if (btnInitial.getText().equals("開始掃描")) {
                        //掃描每個重量位置平均標準差
                        //設定取樣頻率
                        frequencyString = gv.getFrequency();
                        if (frequencyString.equals("50 Hz")) {
                            frequency = 50;
                        } else {
                            frequency = 100;
                        }
                        //開始掃描重量
                        number = 0;
                        writeWeight = "";
                        for (int nowWeight = 0; nowWeight < (gv.getMaxWeight() - gv.getMinWeight()) / gv.getEachWeight() + 1; nowWeight++) {
                            //寫入重量參數的公斤等差
                            writeWeight += (gv.getMinWeight() + gv.getEachWeight() * nowWeight) + ",";

                            AlertDialog.Builder builder = new AlertDialog.Builder(Dual.this);
                            builder.setMessage("請將插銷插在" + (gv.getMaxWeight() - gv.getEachWeight() * nowWeight) + "公斤處點擊確認");
                            builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    initialWeightFlag = true;
                                    txvWeightStack.setText("機器參數設定中");
                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            btnInitial.setText("讀取已設定參數");
                        }
                    }else if(btnInitial.getText().equals("讀取已設定參數")){
                        //讀取已經設定過的參數
                        File tmpDir = new File(MainActivity.CSV_FILE_PATH+"weightParameter.csv");
                        boolean exists = tmpDir.exists();
                        if(exists) {
                            //load the exist file
                            loadWeightParameter();
                            getWeightFlag = true;
                            //設定取樣頻率
                            frequencyString = gv.getFrequency();
                            if (frequencyString == null){
                                frequency = 100;
                            }else {
                                if (frequencyString.equals("50 Hz")) {
                                    frequency = 50;
                                } else {
                                    frequency = 100;
                                }
                            }
                        }else {
                            btnInitial.setText("無設定過參數");
                            Toast.makeText(Dual.this, "請先連線並設定機器參數!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        btnRM = findViewById(R.id.btnRM);
        btnRM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnRM.getText() == "1RM模式"){
                    btnRM.setText("訓練模式");
                    RMflag = false;
                }else{
                    btnRM.setText("1RM模式");
                    RMflag = true;
                }
            }
        });

        btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txvWeightStack.getText()!="機器參數設定中" && getWeightFlag && startFlag == false){
                    getWeightFlag = false;
                    //調整正負五公斤
                    final int weightStack = Integer.valueOf(txvWeightStack.getText().toString());
                    final String[] weight = {Integer.toString(weightStack-5),Integer.toString(weightStack),Integer.toString(weightStack+5)};
                    AlertDialog.Builder dialog_list = new AlertDialog.Builder(Dual.this);
                    dialog_list.setTitle("Stack Weight");
                    dialog_list.setItems(weight, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (weight[which] == Integer.toString(weightStack+5)) { //增加五公斤
                                txvWeightStack.setText(weight[which]);
                            } else if (weight[which] == Integer.toString(weightStack)) { //維持原重
                                txvWeightStack.setText(weight[which]);
                            }else{//增加五公斤
                                txvWeightStack.setText(weight[which]);
                            }
                        }
                    });
                    dialog_list.show();
                }
            }
        });

        editInSec = (EditText)findViewById(R.id.editInSec);
        editOutSec = (EditText)findViewById(R.id.editOutSec);

        btnTempo = findViewById(R.id.btnTempo);
        btnTempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String warning = "";
                if(btnTempo.getText() == "關閉"){
                    if(editInSec.getText().toString().matches("")){
                        warning += "向心時間 ";
                    }
                    if(editOutSec.getText().toString().matches("")){
                        warning += "離心時間 ";
                    }
                    if(warning.length() == 0) {
                        btnTempo.setText("開啟");
                    }else {
                        Toast.makeText(Dual.this, "請輸入" + warning + "!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    btnTempo.setText("關閉");
                }
                Log.d("warning",warning);
            }
        });

        btnStartExercise = findViewById(R.id.btnStartExercise);
        btnStartExercise.setText("Start");
        btnStartExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txvWeightStack.getText().equals("-")||txvWeightStack.getText().equals("機器參數設定中")) {
                    Toast.makeText(Dual.this, "請稍後重量偵測", Toast.LENGTH_LONG).show();
                }else {
                    if (btnStartExercise.getText() == "Start") {
                        startFlag = true;
                        countData = 0;
                        btnStartExercise.setText("Pause");
                        //數值歸零
                        initialTrain();
                    } else {
                        pauseFlag = true;
                        pauseTimeStart = countData;
                        Log.d("pauseStartData", "手動的" + pauseTimeStart);
                        //進入下組訓練或結束訓練(手動)
                        countForRestTime = 0;
                        AlertDialog.Builder builder = new AlertDialog.Builder(Dual.this);
                        builder.setMessage("請問要繼續訓練嗎?");
                        builder.setPositiveButton("完成訓練", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startFlag = false;
                                getWeightFlag = true;
                                btnStartExercise.setText("Start");
                                countGroup = 1;

                                totalTime = pauseTimeStart - totalPauseTime;

                                Log.d("pausetotalTime", "手動的總時間" + totalTime);

                                //計算1RM
                                if(RMflag) {
                                    if (pose.equals("chest")) {
                                        RM = Integer.valueOf((String) txvWeightStack.getText()) * times * chestRM[times - 1];
                                    } else if (pose.equals("leg")) {
                                        RM = Integer.valueOf((String) txvWeightStack.getText()) * times * legRM[times - 1];
                                    }
                                    new AlertDialog.Builder(Dual.this)
                                            .setTitle("1RM預測")
                                            .setMessage("您的1RM預測為 " + (int) RM + " kg")
                                            .show();
                                }

                                totalPauseTime = 0;

                            }
                        }).setNegativeButton("繼續訓練", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                totalPauseTime += (countData - pauseTimeStart);
                                Log.d("pausetotalTime", "手動的總休息時間" + totalPauseTime);
                                pauseFlag = false;
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }
        });

        //資料庫
        DH = new SQLdata(this,null,null,1);
        db = DH.getWritableDatabase();
        select();

        //soundpool
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }else{
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC,0);
        }

        soundtick = soundPool.load(this,R.raw.tick0,1);
        soundtock = soundPool.load(this,R.raw.tock0,1);
        soundup = soundPool.load(this,R.raw.up,1);
        sounddown = soundPool.load(this,R.raw.down,1);

        //確認藍牙是否開啟
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBtAdapter.isEnabled()) {//如果未開啟藍牙
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);//要求開啟藍牙
        }
        initialBluetooth();
        readIdentity();
    }

    private void loadWeightParameter() {
        File inFile = new File(MainActivity.CSV_FILE_PATH+"weightParameter.csv");
        final StringBuilder sb = new StringBuilder();
        String readString;
        String[] arr;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inFile));
            int a = 0;
            while ((readString = reader.readLine()) != null) {
                sb.append(readString);
                if(a == 0){
                    GlobalVariable gv = (GlobalVariable)getApplicationContext();
                    arr = sb.toString().split(",");
                    gv.setMinWeight(Integer.valueOf(arr[0]));
                    gv.setMaxWeight(Integer.valueOf(arr[arr.length-1]));
                    gv.setEachWeight(Integer.valueOf(arr[1])-Integer.valueOf(arr[0]));
                }else if(a == 1) {
                    arr = sb.toString().split(",");
                    for (int i = 0; i<arr.length; i++){
                        average[i] = Double.valueOf(arr[i]);
                    }
                    number = arr.length;
                }else if(a == 2){
                    arr = sb.toString().split(",");
                    for (int i = 0; i<arr.length; i++){
                        deviation[i] = Double.valueOf(arr[i]);
                    }
                }else if(a == 3){
                    GlobalVariable gv = (GlobalVariable)getApplicationContext();
                    arr = sb.toString().split(",");
                    gv.setPose(arr[0]);
                }else if(a == 4){
                    GlobalVariable gv = (GlobalVariable)getApplicationContext();
                    arr = sb.toString().split(",");
                    gv.setFrequency(arr[0]);
                }else if(a == 5){
                    GlobalVariable gv = (GlobalVariable)getApplicationContext();
                    arr = sb.toString().split(",");
                    gv.setTOFHeight(Integer.valueOf(arr[0]));
                }
                a++;
                sb.setLength(0);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if(txvConnectState.getText().equals("已連線")) {
            mService.writeRXCharacteristic("B".getBytes());
            Toast.makeText(Dual.this, "藍牙已斷開", Toast.LENGTH_SHORT).show();
            mService.disconnect();
        }
        super.onBackPressed();
    }

    private void select() {
        //查詢資料庫並載入
        Cursor cursor = db.query("TBHistory", new String[]
                        {"_id","_date","_nowTime","_name","_pose","_group","_times","_weight","_mac",
                                "_distanceIn","_timeIn","_powerIn","_distanceOut","_timeOut","_powerOut"},
                null,null,null,null,null);
        List<Map<String,Object>> items = new ArrayList<Map<String,Object>>();
        cursor.moveToFirst();
        sum_id = cursor.getCount();

        //叫出資料庫的資料
        for(int i = 0; i<sum_id;i++){
            Map<String,Object> item = new HashMap<String,Object>();
            item.put("_id",cursor.getInt(0));
            item.put("_date",cursor.getInt(1));
            item.put("_nowTime",cursor.getString(2));
            item.put("_name",cursor.getString(3));
            item.put("_pose",cursor.getInt(4));
            item.put("_group",cursor.getInt(5));
            item.put("_times",cursor.getDouble(6));
            item.put("_weight",cursor.getDouble(7));
            item.put("_mac",cursor.getDouble(8));
            item.put("_distanceIn",cursor.getDouble(9));
            item.put("_timeIn",cursor.getDouble(10));
            item.put("_powerIn",cursor.getDouble(11));
            item.put("_distanceOut",cursor.getDouble(12));
            item.put("_timeOut",cursor.getDouble(13));
            item.put("_powerOut",cursor.getDouble(14));
            items.add(item);//新增
            cursor.moveToNext();//移到下一筆資料
        }
    }

    private void add(String date,String nowTime,String name,String pose,int group,int times,int weight,String mac,
                     double distanceIn,double timeIn,double powerIn,double distanceOut,double timeOut,double powerOut) {
        db = DH.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("_id",String.valueOf(sum_id+1));
        values.put("_date",date);
        values.put("_nowTime",nowTime);
        values.put("_name",name);
        values.put("_pose",pose);
        values.put("_group",group);
        values.put("_times",times);
        values.put("_weight",weight);
        values.put("_mac",mac);
        values.put("_distanceIn",distanceIn);
        values.put("_timeIn",timeIn);
        values.put("_powerIn",powerIn);
        values.put("_distanceOut",distanceOut);
        values.put("_timeOut",timeOut);
        values.put("_powerOut",powerOut);
        db.insert("TBHistory",null,values);//寫入db
        select();
    }

    private void initialTrain() {
        times = 0;
        timerIn = 0;
        timerOut = 0;
        timerTemp = 0;
        powerIn = 0;
        powerOut = 0;
        GlobalVariable gv = (GlobalVariable)getApplicationContext();
        pose = gv.getPose();
        txvExercise.setText(pose+"\n"+countGroup+"\n"+times + "\n" + (double) timerOut / frequency + "/" + (double) timerIn / frequency + "\n" + df.format(powerOut) + "/" + df.format(powerIn));
        if(countGroup % 2 == 1){
            txvHost.setTextColor(Color.rgb(68,188,216));
            txvGuest.setTextColor(Color.rgb(0,0,0));
        }else{
            txvHost.setTextColor(Color.rgb(0,0,0));
            txvGuest.setTextColor(Color.rgb(68,188,216));
        }
    }

    private final BroadcastReceiver GattUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            //*********************//
            if (action.equals(BLEService.ACTION_GATT_CONNECTED)) {//連線Gatt的廣播訊息
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.e("Action","Gatt Connected");
                    }
                });
            }
            //*********************//
            if (action.equals(BLEService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.writeRXCharacteristic("A".getBytes());
                mService.enableTXNotification();
                Log.e("Action","Find BLE divice.");
            }
            //*********************//
            final TextView textView;
            textView = findViewById(R.id.txvChangeWeight);
            if (action.equals(BLEService.ACTION_DATA_AVAILABLE)) {
                //設定連線狀態
                mState = UART_PROFILE_CONNECTED;
                Log.e("Action","處理資料.");
                txvDeviceName.setText(String.valueOf(mDevice.getAddress()));
                txvConnectState.setText("已連線");
                final Dataformat dataformat = new Dataformat(intent.getByteArrayExtra(BLEService.EXTRA_DATA));
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            countForFrequency++;
                            if (frequency == 50 && (countForFrequency % 2 == 0)){
                                //讀取Tof距離資料
                                TofData = dataformat.result;
                                if(dataformat.count == 99){
                                    mService.writeRXCharacteristic("A".getBytes());
                                }
                                textView.setText(Integer.toString(TofData));

                                //設定各重量的高度平均值和標準差
                                if(initialWeightFlag) {
                                    //紀錄五秒內的數值計算平均值和標準差
                                    scanWeight(5);
                                }

                                //即時判斷重量
                                if(getWeightFlag && !startFlag){
                                    realTimeGetWeight();
                                }

                                if(startFlag) {
                                    //偵測次數
                                    peakDetection();
                                    //運動節奏
                                    int secIn = 0,secOut = 0;
                                    if(btnTempo.getText() == "開啟"){
                                        secIn = Integer.valueOf(editInSec.getText().toString());
                                        secOut = Integer.valueOf(editOutSec.getText().toString());
                                        exerciseTempo(secIn,secOut);
                                    }else{
                                        countForRestTime++;
                                    }
                                }

                                //休息讀秒
                                if(!startFlag && countGroup != 1){
                                    if(countForRestTime % frequency == 0) {
                                        mToast = Toast.makeText(Dual.this, "休息" + Integer.valueOf(countForRestTime / frequency) + "秒", Toast.LENGTH_SHORT);
                                        mToast.show();
                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mToast.cancel();
                                            }
                                        }, 800);
                                    }
                                }

                            }else{
                                //讀取Tof距離資料
                                TofData = dataformat.result;
                                if(dataformat.count == 99){
                                    mService.writeRXCharacteristic("A".getBytes());
                                }
                                textView.setText(Integer.toString(TofData));

                                //設定各重量的高度平均值和標準差
                                if(initialWeightFlag) {
                                    //紀錄五秒內的數值計算平均值和標準差
                                    scanWeight(5);
                                }

                                //即時判斷重量
                                if(getWeightFlag && !startFlag){
                                    realTimeGetWeight();
                                }

                                if(startFlag) {
                                    //偵測次數
                                    peakDetection();
                                    //運動節奏
                                    int secIn,secOut;
                                    if(btnTempo.getText() == "開啟"){
                                        secIn = Integer.valueOf(editInSec.getText().toString());
                                        secOut = Integer.valueOf(editOutSec.getText().toString());
                                        exerciseTempo(secIn,secOut);
                                    }
                                }else{
                                    countForRestTime++;
                                }
                                //休息讀秒
                                if(!startFlag && countGroup != 1){
                                    if(countForRestTime % frequency == 0) {
                                        mToast = Toast.makeText(Dual.this, "休息" + Integer.valueOf(countForRestTime / frequency) + "秒", Toast.LENGTH_SHORT);
                                        mToast.show();
                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                mToast.cancel();
                                            }
                                        }, 800);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                    private void exerciseTempo(int secIn,int secOut) {
                        if(countData % frequency == 0){
                            if(countSound == 0) {
                                soundPool.play(sounddown, 1, 1, 0, 0, 1);
                            }else if(countSound == secIn + 1){
                                soundPool.play(soundup,1,1,0,0,1);
                            }else{
                                soundPool.play(soundtick,1,1,0,0,1);
                            }
                            countSound++;
                            if(countSound > secIn+secOut+1){
                                countSound = 0;
                            }
                        }
                    }

                    //即時判斷重量
                    private void realTimeGetWeight() {
                        double tempDistance1 = Math.abs(TofData - average[0]);
                        double tempDistance2;
                        GlobalVariable gv = (GlobalVariable)getApplicationContext();
                        int tempWeight = gv.getMinWeight();

                        //判斷是哪個重量方法
                        //number是average array根據對應重量的位置
                        //tempDistance1儲存目前最短的距離
                        for(int i=0;i<number-1;i++){
                            tempDistance2 = Math.abs(TofData - average[i+1]);
                            if(tempDistance1 > tempDistance2){
                                tempDistance1 = tempDistance2;
                                tempWeight = gv.getMinWeight()+gv.getEachWeight()*(i + 1);
                            }
                        }
                        txvWeightStack.setText(String.valueOf(tempWeight));
                    }

                    //掃描sec秒資料後計算平均值以及標準差
                    public void scanWeight(int sec) {
                        getWeightFlag = false;
                        if(count < sec*frequency) {
                            saveData[count] = TofData;
                            count++;
                        }else{
                            //計算平均值
                            for(int i = 0; i< saveData.length; i++){
                                tempAverage += saveData[i];
                            }
                            tempAverage = tempAverage/saveData.length;

                            //計算標準差
                            for(int i = 0; i<saveData.length; i++){
                                tempDeviation += Math.pow(saveData[i]-tempAverage,2);
                            }
                            tempDeviation = tempDeviation/(saveData.length-1);
                            tempDeviation = Math.sqrt(tempDeviation);

                            average[number] = tempAverage;
                            deviation[number] = tempDeviation;

                            soundPool.play(soundtick,1,1,0,0,1);

                            //儲存機器參數檔案
                            try {
                                bw = CsvWriter.createBW("weightParameter.csv");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                String writeAverage = "",writeDeviation = "";
                                for(int i = 0;i <= number;i++){
                                    writeAverage += (average[i]+",");
                                    writeDeviation += (deviation[i]+",");
                                }
                                bw.write(writeWeight);
                                bw.newLine();
                                bw.write(writeAverage);
                                bw.newLine();
                                bw.write(writeDeviation);
                                bw.newLine();
                                bw.write(gv.getPose());
                                bw.newLine();
                                bw.write(gv.getFrequency());
                                bw.newLine();
                                bw.write(String.valueOf(gv.getTOFHeight()));
                                bw.newLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            CsvWriter.saveCsv(bw);

                            number++;
                            count = 0;
                            tempAverage = 0;
                            tempDeviation = 0;
                            initialWeightFlag = false;
                            getWeightFlag = true;
                        }


                    }

                    GlobalVariable gv = (GlobalVariable)getApplicationContext();
                    int HEIGHT = gv.getTOFHeight();
                    int m;
                    double g,h,v;
                    //紀錄開始時間(資料庫)
                    Date dNow = new Date( );
                    SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy.MM.dd");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

                    private void peakDetection() {
                        //取斜率
                        if (countData == 0) {
                            preTofData = TofData;
                            //開始的時間
                            dNow = new Date();
                        } else if (countData == 1) {
                            preSlope = TofData - preTofData;
                            preTofData = TofData;
                        } else {
                            preSlope = nowSlope;
                            nowSlope = TofData - preTofData;
                        }
                        totalPauseTime = 0;

                        //斜率<0維波峰和波谷 若偵測到波峰或波谷和前一個峰值比較 差值大於200(20cm)即為peak
                        if (preSlope * nowSlope <= 0) {
                            //peakFlag為前一個峰值(可能是波峰或波谷)若波間距離>200即為下一個波值(濾掉小雜訊)
                            //當peakFlag為波峰TofData為波谷
                            if (peakFlag - TofData >= 200) {
                                //當peakFlag為波峰TofData為波谷 = TofData;
                                times++;
                                //功率 = mgh+0.5*m*v*v*h
                                timerIn = countData - timerTemp;
                                timerTemp = countData;

                                m = Integer.valueOf((String)txvWeightStack.getText());
                                g = 9.8;
                                h = (float)(HEIGHT-TofData)/1000.0;
                                v = (float)(peakFlag - TofData)/1000.0/((float) timerIn/(float)frequency);
                                powerIn = m*g*h + 0.5*m*v*v*h;
                                distanceIn = (double)(TofData - peakFlag)/1000.0;

                                peakFlag = TofData;
                            }
                            //當peakFlag為波谷TofData為波峰
                            if (peakFlag - TofData <= -200) {
                                //功率 = mgh+0.5*m*v*v*h
                                timerOut = countData - timerTemp;
                                timerTemp = countData;

                                m = Integer.valueOf((String)txvWeightStack.getText());
                                g = 9.8;
                                h = (float)(HEIGHT-TofData)/1000.0;
                                v = (float)(TofData - peakFlag)/1000.0/((float) timerOut/(float)frequency);
                                powerOut = m*g*h + (-0.5)*m*v*v*h;
                                distanceOut = (double)(TofData - peakFlag)/1000.0;

                                peakFlag = TofData;
                                //儲存每一下到資料庫(第0下不記)
                                // tempTimes紀錄上次寫入的次數，避免重複
                                if(times != 0 && times !=tempTimes) {
                                    if(countGroup % 2 == 1) {
                                        add(dateFormat.format(dNow), timeFormat.format(dNow), name, pose, countGroup, times, Integer.valueOf((String) txvWeightStack.getText()), txvDeviceName.getText().toString(), distanceIn, (double) timerIn / (double) frequency, powerIn, distanceOut, (double) timerOut / (double) frequency, powerOut);
                                    }else{
                                        add(dateFormat.format(dNow), timeFormat.format(dNow), nameGuest, pose, countGroup, times, Integer.valueOf((String) txvWeightStack.getText()), txvDeviceName.getText().toString(), distanceIn, (double) timerIn / (double) frequency, powerIn, distanceOut, (double) timerOut / (double) frequency, powerOut);
                                    }
                                    tempTimes = times;
                                }
                                Date dNow = new Date( );
                            }
                            GlobalVariable gv = (GlobalVariable)getApplicationContext();
                            pose = gv.getPose();
                            txvExercise.setText(pose+"\n"+countGroup+"\n"+times + "\n" + (double) timerIn / frequency + "/" + (double) timerOut / frequency + "\n" + df.format(powerIn) + "/" + df.format(powerOut));

                            //切換使用者(顏色)
                            if(countGroup % 2 == 1){
                                txvHost.setTextColor(Color.rgb(68,188,216));
                                txvGuest.setTextColor(Color.rgb(0,0,0));
                            }else{
                                txvHost.setTextColor(Color.rgb(0,0,0));
                                txvGuest.setTextColor(Color.rgb(68,188,216));
                            }
                        }
                        preTofData = TofData;
                        countData++;

                        //自動離開時間(sec)
                        if(!pauseFlag) {
                            leaveTime(10);
                        }
                    }

                    private void leaveTime(int i) {
                        //判斷運動結束(i秒)
                        if(countData == 0){
                            restTimeStart = countData;
                            restTofData = TofData;
                        }else{
                            if(Math.abs(TofData-restTofData) <= 10){
                                restTimeCount++;
                            }else{
                                restTimeCount = 0;
                                restTimeStart = countData;
                                restTofData = TofData;
                            }
                        }
                        //顯示靜止讀秒
                        if((restTimeCount < i*frequency) && (restTimeCount % frequency == 0) && restTimeCount != 0){
                            mToast = Toast.makeText(Dual.this, "靜止" + Integer.valueOf(restTimeCount / frequency) + "秒", Toast.LENGTH_SHORT);
                            mToast.show();
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mToast.cancel();
                                }
                            }, 800);
                        }else if(restTimeCount == i*frequency){
                            //計算1RM
                            if(RMflag) {
                                if (pose.equals("chest")) {
                                    RM = Integer.valueOf((String) txvWeightStack.getText()) * times * chestRM[times - 1];
                                } else if (pose.equals("leg")) {
                                    RM = Integer.valueOf((String) txvWeightStack.getText()) * times * legRM[times - 1];
                                }
                            }

                            totalTime = countData-totalPauseTime;
                            Log.d("pausecountData","自動的Count"+countData);
                            Log.d("pausetotalTime","自動的總時間"+totalTime);

                            //進入下組訓練或結束訓練(自動)
                            countForRestTime = 0;
                            AlertDialog.Builder builder = new AlertDialog.Builder(Dual.this);
                            builder.setMessage("請問要繼續訓練嗎?");
                            builder.setPositiveButton("下一組", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    countForRestTime = 0;
                                    countGroup++;
                                    initialTrain();
                                }
                            }).setNegativeButton("完成訓練", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    countGroup = 1;
                                    if(RMflag) {
                                        new AlertDialog.Builder(Dual.this)
                                                .setTitle("1RM預測")
                                                .setMessage("您的1RM預測為 " + (int) RM + " kg")
                                                .show();
                                    }
                                    totalPauseTime = 0;
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();

                            //重置離開時間辨識係數
                            startFlag = false;
                            countData = 0;
                            restTimeCount = 0;
                            restTofData = 0;
                            btnStartExercise.setText("Start");
                        }
                    }
                });
            }
            if (action.equals(BLEService.DEVICE_DOES_NOT_SUPPORT_UART)){
                mService.disconnect();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(GattUpdateReceiver);
        } catch (Exception ignore) {
            Log.e("EX", ignore.toString());
        }
        unbindService(mServiceConnection);
        Broadcast_ReceiverFlag = 0;
        mService.stopSelf();
        mService= null;
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.e(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        if(Broadcast_ReceiverFlag == 0)
        {
            Log.e(TAG, "Register_Broadcast_Receiver");
            LocalBroadcastManager.getInstance(this).registerReceiver(GattUpdateReceiver, makeGattUpdateIntentFilter());
            Broadcast_ReceiverFlag = 1;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    private void initialBluetooth() {
        //藍牙
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(Dual.this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        }
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Intent bindIntent = new Intent(this, BLEService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((BLEService.LocalBinder) rawBinder).getService();

            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            //     mService.disconnect(mDevice);
            mService = null;
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //查看ScanQRcode是否正確結束(REQUEST_CODE=1正確)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String returnMac = data.getStringExtra("returnString");
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(returnMac);
                Log.e("MainonActivityResult OK", "... onActivityResultdevice.address==" + mDevice+ "mserviceValue" + mService);
                mService.connect(returnMac);//連線
                txvConnectState.setText("尋找藍牙裝置");

                //掃描結束 詢問是否觀看教學影片
                new AlertDialog.Builder(this)
                        .setMessage("請問是否要觀看教學影片")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setClass(Dual.this, YoutubePlayer.class);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        }
    }

    private void readIdentity() {
        //讀取identityHost.csv取得Host個資
        File inFileHost = new File(MainActivity.CSV_FILE_PATH+"identityHost.csv");
        final StringBuilder sbHost = new StringBuilder();
        String readStringHost;
        String[] arrHost;
        try {
            BufferedReader readerHost = new BufferedReader(new FileReader(inFileHost));
            int a = 0;
            while ((readStringHost = readerHost.readLine()) != null) {
                a++;
                //略過標頭
                if(a == 2) {
                    sbHost.append(readStringHost);
                    arrHost = sbHost.toString().split(",");
                    for (int i = 0; i < arrHost.length; i++) {
                        if(i == 0){
                            name = arrHost[i];
                        }else if(i == 1){
                            height = Integer.parseInt(arrHost[i].trim());;
                        }else if(i == 2){
                            weight = Integer.parseInt(arrHost[i].trim());;
                        }else if(i == 3){
                            age = Integer.parseInt(arrHost[i].trim());;
                        }else if(i == 4){
                            gender = arrHost[i];
                        }
                    }
                    //顯示使用者資訊
                    txvHost = findViewById(R.id.txvHost);
                    txvHost.setText(name+"\n"+height+"\n"+weight+"\n"+age+"\n"+gender+"\ndual");
                }
            }
            readerHost.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //讀取identityGuest.csv取得Guest個資
        File inFileGuest = new File(MainActivity.CSV_FILE_PATH+"identityGuest.csv");
        final StringBuilder sbGuest = new StringBuilder();
        String readStringGuest;
        String[] arrGuest;
        try {
            BufferedReader readerGuest = new BufferedReader(new FileReader(inFileGuest));
            int a = 0;
            while ((readStringGuest = readerGuest.readLine()) != null) {
                a++;
                //略過標頭
                if(a == 2) {
                    sbGuest.append(readStringGuest);
                    arrGuest = sbGuest.toString().split(",");
                    for (int i = 0; i < arrGuest.length; i++) {
                        if(i == 0){
                            nameGuest = arrGuest[i];
                        }else if(i == 1){
                            heightGuest = Integer.parseInt(arrGuest[i].trim());;
                        }else if(i == 2){
                            weightGuest = Integer.parseInt(arrGuest[i].trim());;
                        }else if(i == 3){
                            ageGuest = Integer.parseInt(arrGuest[i].trim());;
                        }else if(i == 4){
                            genderGuest = arrGuest[i];
                        }
                    }
                    //顯示使用者資訊
                    txvGuest = findViewById(R.id.txvGuest);
                    txvGuest.setText(nameGuest+"\n"+heightGuest+"\n"+weightGuest+"\n"+ageGuest+"\n"+genderGuest+"\ndual");
                }
            }
            readerGuest.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

