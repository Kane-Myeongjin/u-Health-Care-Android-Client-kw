package malid.datacollector.Activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Set;

import malid.datacollector.CounterService;
import malid.datacollector.Helpers.CustomBluetoothProfile;
import malid.datacollector.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


// 처음의 운동강도 선택을 해야지 값이 입력됨.
//바꿔야함
public class MainActivity extends AppCompatActivity{

    private static final String TAG="U-Health-main";
    private static final String SERVER_URL_ADDRESS="http://13.125.151.92:9000/post";

    //////////////////// 디바이스 블루투스 연결 변수 ////////////////////
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    BluetoothDevice bluetoothDevice;

    Button btnStartConnecting, btnServer;
    EditText txtPhysicalAddress;
    TextView txtState, txtByte, textServer;
    //////////////////// 디바이스 블루투스 연결 변수 ////////////////////

    //////////////////// 1차 변수 ////////////////////
    private HRThread hrthread = new HRThread();
    private Thread thread;
    private MediaPlayer mMusicPlayer;
    private CounterService binder;
    private boolean running = false;

    private int time=0;
    private int prev_step=0, prev_distance=0, prev_cal=0;
    private int curr_step=0, curr_distance=0, curr_cal=0;
    private int end =0;

    private ArrayList<Integer> HR_list = new ArrayList<Integer>();
    //////////////////// 1차 변수 ////////////////////


    //////////////////// 2차 변수 ////////////////////
    //전송 data
    private int mDegree=0;
    private int mHeartRate=0;
    private int mUserSessionId;

    // 임시 사용 데이터 @@@@@@@
    private int mStanHR=50;

    //widget
    private RadioGroup mGroupDegree;
    private Chronometer mChronometer;
    private TextView mTvDegree;
    private TextView mTvCurrHR;
    private TextView mTvStanHR;
    private TextView mTvTotalState;
    private ProgressBar mProgState;
    private ToggleButton mToggleMusic;

    float speed = 1f;                   //music speed / default = 1f
    int predict =0;

    //sys
    private boolean mConnDevice;
    //////////////////// 2차 변수 ////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent intent = getIntent();
        mUserSessionId = intent.getExtras().getInt("sid");
        Toast.makeText(getApplicationContext(), "auth sid:"+Integer.toString(mUserSessionId), Toast.LENGTH_LONG).show();



        int uiOptions = this.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        //뒤로가기 버튼 추가
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        //음악추가 초기화 부분
        mMusicPlayer = MediaPlayer.create(this, R.raw.kalimba);
        mConnDevice=false;

        //setPlayPauseButton();

        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("mode", "Turning immersive mode mode off.");
        } else {
            Log.i("mode", "Turning immersive mode mode on.");
        }
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

        initializeObjects();
        initilaizeComponents();
        initializeEvents();

        getBoundedDevice();
    }



    // 뒤로가기 버튼 이벤트 처리 리스너
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void changeplayerSpeed(float speed) {
        // this checks on API 23 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mMusicPlayer.isPlaying()) {
                mMusicPlayer.setPlaybackParams(mMusicPlayer.getPlaybackParams().setSpeed(speed));
                Toast.makeText(getApplicationContext(),"@@", Toast.LENGTH_SHORT).show();

            }
        }
    }


    // MI Band 2 MAC address 자동등록
    void getBoundedDevice() {
        Set<BluetoothDevice> boundedDevice = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : boundedDevice) {
            if (bd.getName().contains("MI Band 2")) {
                txtPhysicalAddress.setText(bd.getAddress());
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 서비스가 가진 binder를 리턴 받음
            binder = CounterService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    void initializeObjects() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    void initilaizeComponents() {
        btnStartConnecting = (Button) findViewById(R.id.btnStartConnecting);
        btnServer = (Button) findViewById(R.id.btnStart);
        txtPhysicalAddress = (EditText) findViewById(R.id.txtPhysicalAddress);
        txtState = (TextView) findViewById(R.id.textConn);
        txtByte = (TextView) findViewById(R.id.textByte);
        textServer = (TextView) findViewById(R.id.textServer);

        //////// comteam update //////
        mTvDegree = (TextView)findViewById(R.id.textDegree);
        mTvCurrHR = (TextView)findViewById(R.id.textCurr);
        mTvTotalState = (TextView)findViewById(R.id.textState);
        mTvStanHR = (TextView)findViewById(R.id.textStan);
        mChronometer = (Chronometer)findViewById(R.id.chronometer);
        mProgState = (ProgressBar)findViewById(R.id.progressState);
        //라디오 그룹 초기화 degree//
        int id = R.id.radioDegree1; //디폴트 값 초록
        mGroupDegree = (RadioGroup)findViewById(R.id.groupDegree);
        mGroupDegree.check(id); //체크해놓고
        mToggleMusic = (ToggleButton)findViewById(R.id.toggleMusic);
    }

    void initializeEvents() {
        btnStartConnecting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnecting();
            }
        });

        /// 서버전송 버튼 리스너
        btnServer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {


                if(!mConnDevice){
                    Toast.makeText(getApplicationContext(),"디바이스가 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
                    return false;
                }


                if(running == false) {   // 서버전송 시작
                    prev_step=0;
                    prev_distance=0;
                    prev_cal=0;
                    curr_step=0;
                    curr_distance=0;
                    curr_cal=0;
                    mHeartRate = 0;
                    HR_list.clear();
                    txtByte.setText("app:on");
                    textServer.setText("server:ok");

                    //크로노 미터 시작
                    mChronometer.setBase(SystemClock.elapsedRealtime()); //크로노 미터 초기화
                    mChronometer.start();

                    Toast.makeText(getApplicationContext(),"운동 시작", Toast.LENGTH_SHORT).show();
                    btnServer.setText("Stop");
                    getInformation();

                    // Intent intent = new Intent(MainActivity.this, MyCounterService.class);
                    //startService(intent);
                    // bindService(intent, connection, BIND_AUTO_CREATE);
                    running = true;
                    // new Thread(new GetCountThread()).start();
                    thread = new Thread(hrthread);
                    thread.start();
                }
                else {      // 서버전송 종료
                    Toast.makeText(getApplicationContext(),"운동 종료", Toast.LENGTH_SHORT).show();
                    btnServer.setText("Start");
                    mChronometer.setBase(SystemClock.elapsedRealtime()); //크로노 미터 초기화
                    mChronometer.stop();
                    getInformation();

                    // unbindService(connection);
                    running = false;
                    thread.interrupt();
                }

                return true;
            }
        });
    }

    void sendServer() {
        //txtByte.setText("prev info. time["+time+"]sec/"+"HR"+HR_list.toString());
        txtByte.setText("app:off");
        textServer.setText("server:   ");
    }
    void getInformation() { // 걸음수, 거리, 칼로리 정보
        BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.Information.service)
                .getCharacteristic(CustomBluetoothProfile.Information.Characteristic);
        if (!bluetoothGatt.readCharacteristic(bchar)) {
            Toast.makeText(this, "Failed get information info", Toast.LENGTH_SHORT).show();
        }
    }

    public class CountAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            time = 0;
        }
        @Override
        protected String doInBackground(String... urls) {
            while(running){
                try {
                    Thread.sleep(1000); // 1초 대기
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                time ++;    // time 1증가
                publishProgress();      // onProgressUpdate 호출

                if(time!=0 && time%1==0) try {

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.accumulate("HeartRate", mHeartRate);
                    jsonObject.accumulate("time", time);
                    jsonObject.accumulate("degree", mDegree);
                    jsonObject.accumulate("end", end);
                    jsonObject.accumulate("id", mUserSessionId);

                    HttpURLConnection con = null;
                    BufferedReader reader = null;

                    try {
                        URL url = new URL(urls[0]);
                        con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Cache-Control", "no-cache");
                        con.setRequestProperty("Content-Type", "application/json");
                        con.setRequestProperty("Accept", "text/html");
                        con.setDoOutput(true);
                        con.setDoInput(true);
                        con.connect();

                        OutputStream outStream = con.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                        writer.write(jsonObject.toString());
                        writer.flush();
                        writer.close();

                        InputStream stream = con.getInputStream();

                        reader = new BufferedReader(new InputStreamReader(stream));

                        StringBuffer buffer = new StringBuffer();

                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }
                        //textServer.setText("server:"+buffer.toString());//서버로 부터 받은 문자 textView에 출력

                        predict = Integer.parseInt(buffer.toString());

                        //mTvStanHR.setText("[  " + predict + "  ]");

                        if (predict != -1) {
                            mTvStanHR.setText("[  " + (predict+1) + "  ]");
                            // Toast.makeText(getApplicationContext(), "@@@@@", Toast.LENGTH_SHORT).show();
                            if (predict > mDegree){
                                speed = 0.5f;
                                mTvStanHR.setTextColor(Color.parseColor("#FF0000")); //R
                            }
                            else if (predict < mDegree) {
                                speed = 2f;
                                mTvStanHR.setTextColor(Color.parseColor("#0000FF")); //B
                            } else{
                                speed = 1f;
                                mTvStanHR.setTextColor(Color.parseColor("#00FF00")); //G
                            }

                            changeplayerSpeed(speed);
                        }

                        Log.v(TAG, "receive data from server");
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... params) {
            //mTvCurrHR.setText("  "+Integer.toString(mHeartRate)+" BPM");

            //total 상태창 업데이트
            if(mDegree<predict){ //설정값<예측값
                mTvTotalState.setText(Integer.toString(mHeartRate)+" BPM");
                mTvTotalState.setTextColor(Color.parseColor("#FF0000")); //red

            } else {
                mTvTotalState.setText(Integer.toString(mHeartRate)+" BPM");
                mTvTotalState.setTextColor(Color.parseColor("#3aa929")); //green
            }

            if(time!=0 && time%5==0) {
                HR_list.add(mHeartRate);
            }
            mProgState.setProgress(mHeartRate);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }




    //심박수 가져옴
    private class HRThread implements Runnable {

        @Override
        public void run() {

            try{
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while(running){
                    try {
                        startScanHeartRate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch(Exception e) {
            } finally {
                Log.v(TAG, "HR-Thread is dead");
            }

        }
    }



    // 미 밴드 연결
    void startConnecting() {

        String address = txtPhysicalAddress.getText().toString();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        Log.v(TAG, "Connecting to " + address);
        Log.v(TAG, "Device name " + bluetoothDevice.getName());

        bluetoothGatt = bluetoothDevice.connectGatt(this, true, bluetoothGattCallback);

    }

    //연결 됬을때
    void stateConnected() {
        mConnDevice=true;
        bluetoothGatt.discoverServices();
        txtState.setText("Connected");
    }

    //연결 안됬을 때
    void stateDisconnected() {
        mConnDevice=false;
        bluetoothGatt.disconnect();
        txtState.setText("Disconnected");
    }

    void startScanHeartRate() {
        BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                .getCharacteristic(CustomBluetoothProfile.HeartRate.controlCharacteristic);
        bchar.setValue(new byte[]{21, 1, 1});       // 대략 10번 측정
        // new byte[]{21, 2, 1} -> 1번 측정
        bluetoothGatt.writeCharacteristic(bchar);
    }

    void listenHeartRate() {
        BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                .getCharacteristic(CustomBluetoothProfile.HeartRate.measurementCharacteristic);
        bluetoothGatt.setCharacteristicNotification(bchar, true);
        BluetoothGattDescriptor descriptor = bchar.getDescriptor(CustomBluetoothProfile.HeartRate.descriptor);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }




    final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v(TAG, "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                stateConnected();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stateDisconnected();
            }

        }

        @Override   // mGatt.discoverServices(); 사용시 호출
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v(TAG, "onServicesDiscovered");
            listenHeartRate();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.v(TAG, "onCharacteristicRead");
            byte[] data = characteristic.getValue();

            if(data.length == 13) {
                int new_step = (data[4]&0xFF) << 24 | (data[3] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[1] & 0xFF);
                int new_distance = (data[8]&0xFF) << 24 | (data[7] & 0xFF) << 16 | (data[6] & 0xFF) << 8 | (data[5] & 0xFF);
                int new_cal = (data[12]&0xFF) << 24 | (data[11] & 0xFF) << 16 | (data[10] & 0xFF) << 8 | (data[9] & 0xFF);

                if(running == true){
                    prev_step = new_step;
                    prev_distance = new_distance;
                    prev_cal = new_cal;
                }
                else{
                    curr_step = new_step;
                    curr_distance = new_distance;
                    curr_cal = new_cal;
                    sendServer();
                }

                Log.v(TAG, "step : "+new_step);
                Log.v(TAG, "distance : "+new_distance+"m");
                Log.v(TAG, "cal : "+new_cal);

                //txtByte.setText("step : " +step+"\n" + "distance : " + distance + "m\n"+"cal : " + cal);
            }


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.v(TAG, "onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] data = characteristic.getValue();
            if(data.length == 2){
                Log.v(TAG, "HR : "+data[1]);
                //if(running) HR_list.add(data[1]&0xFF);
                if(mHeartRate == 0 && ((data[1]&0xFF)!=0)){ // 처음 심박수를 읽은 경우
                    mHeartRate = data[1]&0xFF;

                    CountAsyncTask myAsyncTask = new CountAsyncTask();  // count 스레드생성
                    myAsyncTask.execute(SERVER_URL_ADDRESS);  // count 스레드실행
                }
                else if((data[1]&0xFF)!=0){
                    mHeartRate = data[1]&0xFF;
                }

            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorWrite");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.v(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.v(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.v(TAG, "onMtuChanged");
        }

    };


    //위젯 아이디 분류 함수
    public void mOnClick(View v){
        switch(v.getId()){
            //플레이어 토글버튼 아이디 파인더
            case R.id.toggleMusic:
                mPlayerByToggle();
                break;

            //Degree 라디오 버튼 수정부분
            case R.id.radioDegree1:
                mDegree=0;
                mSetDegreeText(0);
                //mSetStanHRText(0);
                mTvCurrHR.setText("[  " + (mDegree+1) + "  ]");
                break;
            case R.id.radioDegree2:
                mDegree=1;
                mSetDegreeText(1);
                //mSetStanHRText(1);
                mTvCurrHR.setText("[  " + (mDegree+1) + "  ]");
                break;
            case R.id.radioDegree3:
                mDegree=2;
                mSetDegreeText(2);
                //mSetStanHRText(2);
                mTvCurrHR.setText("[  " + (mDegree+1) + "  ]");
                break;
            case R.id.radioDegree4:
                mDegree=3;
                mSetDegreeText(3);
                // mSetStanHRText(3);
                mTvCurrHR.setText("[  " + (mDegree+1) + "  ]");
                break;
            case R.id.radioDegree5:
                mDegree=4;
                mSetDegreeText(4);
                // mSetStanHRText(4);
                mTvCurrHR.setText("[  " + (mDegree+1) + "  ]");
                break;
        }
    }

    private void mSetDegreeText(int degree) {
        switch (degree){
            case 0:
                mTvDegree.setText("1. 매우약함");
                break;
            case 1:
                mTvDegree.setText("2. 약함");
                break;
            case 2:
                mTvDegree.setText("3. 중간");
                break;
            case 3:
                mTvDegree.setText("4. 약간강함");
                break;
            case 4:
                mTvDegree.setText("5. 강함");
                break;
        }
    }



    private void mPlayerByToggle(){
        if(mToggleMusic.isChecked()) { //MUSIC ON
            mMusicPlayer.start();
            changeplayerSpeed(speed);
        } else { //MUSIC OFF
            mMusicPlayer.pause();
        }
    }



}