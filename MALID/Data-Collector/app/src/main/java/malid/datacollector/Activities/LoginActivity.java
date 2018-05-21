package malid.datacollector.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

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

import malid.datacollector.Modules.BackPressCloseHandler;
import malid.datacollector.Modules.SaveSharedPreference;
import malid.datacollector.R;

    public class LoginActivity extends AppCompatActivity {

        private static final String TAG="U-Health-login";
        private static final String LOGIN_URL_ADDRESS="http://13.125.151.92:9000/login";
        //private static final String TEST_URL_ADDRESS="http://13.125.217.245:3000/login";

        //뒤로가기 종료 시간 체크
        //private final long FINISH_INTERVAL_TIME = 2000;
        //private long backPressedTime = 0;



        //widget
        private Button mBtnSignIn, mBtnSignUp;
        private EditText mEditUid, mEditUpwd;


        //member data
        private String mUserId, mUserPwd;
        private boolean mAutoSignIn, mRememberId;

        //system data
        private String mServerMsg;
        private BackPressCloseHandler backPressCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.v(TAG, "loginActivity onCreate");

        //액션 바 숨김
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        backPressCloseHandler = new BackPressCloseHandler(this);

        mInitComponents();
        mInitVariables();
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }





        /////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////// 멤버 초기화 함수   ////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////

    private void mInitComponents(){
        mEditUid = (EditText)findViewById(R.id.editUid);
        mEditUpwd = (EditText)findViewById(R.id.editUpwd);
    }
    private void mInitVariables(){
        mUserId = null;
        mUserPwd = null;
        mAutoSignIn = false;
        mRememberId = false;
    }

        /////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////// 리스너 함수  ////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////


    // 버튼 리스너 //
    public void mOnClick(View v){
        switch (v.getId()){
            case R.id.btnSignIn:
                mOnSignInBtn();
                break;
            case R.id.btnSignUp:
                mOnSignUpBtn();
                break;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 이벤트 처리 함수 ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////


    // 로그인 버튼 //
    private void mOnSignInBtn(){
        mInitVariables();
        //아이디 비밀번호 얻어옴
        mUserId = mEditUid.getText().toString();
        mUserPwd = mEditUpwd.getText().toString();

        //예외 : 둘중 하나라도 입력 안했을 경우
        if(mUserId==null || mUserPwd==null){
            Toast.makeText(getApplicationContext(),"아이디 또는 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
        }
        //서버 연결 스레드 실행
        ConnServerAsyncTask connServerAsyncTask = new ConnServerAsyncTask();
        connServerAsyncTask.execute(LOGIN_URL_ADDRESS);
    }

    // 회원가입 버튼 //
    private void mOnSignUpBtn() {
        Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
        startActivity(intent);
    }



    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 서버 연결 스레드 ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    public class ConnServerAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            mServerMsg=null;
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls) {
            try {

                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("UserId", mUserId);
                jsonObject.accumulate("UserPwd", mUserPwd);

                HttpURLConnection con = null;
                BufferedReader reader = null;

                try{
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
                    while((line = reader.readLine()) != null){
                        buffer.append(line);
                    }
                    mServerMsg = buffer.toString();
                    Log.v(TAG, "receive data from server");
                    return mServerMsg;
                } catch (MalformedURLException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(con != null){
                        con.disconnect();
                    }
                    try {
                        if(reader != null){
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }



        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(mServerMsg.equals("err")){
                Toast.makeText(getApplicationContext(), mServerMsg, Toast.LENGTH_SHORT).show();
            } else if(mServerMsg.equals("nack")){
                Toast.makeText(getApplicationContext(), "아이디 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                SaveSharedPreference.setUserName(LoginActivity.this, mServerMsg); //로그인 인증
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(intent);
            }
        }
    }

}
