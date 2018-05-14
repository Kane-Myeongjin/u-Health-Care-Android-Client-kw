package malid.datacollector.Activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
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

import malid.datacollector.R;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG="U-Health-signup";
    private static final String SIGNUP_URL_ADDRESS="http://13.125.151.92:9000/signup";

    // data
    private String mUserId, mUserPwd1, mUserPwd2,
            mUserName, mUserAge, mUserStature, mUserWeight;
    private boolean mUserIsMale;

    // widget
    private EditText mEditUserId, mEditUserPwd1, mEditUserPwd2,
            mEditUserName, mEditUserAge, mEditUserStature, mEditUserWeight;

    private RadioGroup mGroupSex;

    // system
    private String mServerMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //액션 바 숨김
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        mInitComponents();
        mInitVariables();
    }

    private void mInitComponents(){
        mEditUserId = (EditText)findViewById(R.id.editSignupID);
        mEditUserPwd1 = (EditText)findViewById(R.id.editSignupPWD);
        mEditUserPwd2 = (EditText)findViewById(R.id.editSignupPWD2);
        mEditUserName = (EditText)findViewById(R.id.editSignupName);
        mEditUserAge = (EditText)findViewById(R.id.editSignupAge);
        mEditUserStature = (EditText)findViewById(R.id.editSignupStature);
        mEditUserWeight = (EditText)findViewById(R.id.editSignupWeight);
        mGroupSex = (RadioGroup)findViewById(R.id.groupSex);
        mGroupSex.check(R.id.radioMale);
    }
    private void mInitVariables(){
        mUserId = null;
        mUserPwd1 = null;
        mUserPwd2 = null;
        mUserName = null;
        mUserAge = null;
        mUserStature = null;
        mUserWeight = null;
        mUserIsMale = true;
        mServerMsg = null;
    }




    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 리스너 함수  ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    public void mOnClick(View v){
        switch (v.getId()){
            case R.id.btnSignupOK:
                mOnBtnSignupOk();
                break;

            case R.id.btnSignupCancel:
                mOnBtnSignupCancel();
                break;

            case R.id.radioMale:
                mOnRadioBtn(v.getId());
                break;

            case R.id.radioFemale:
                mOnRadioBtn(v.getId());
                break;

        }
    }


    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 이벤트 처리 함수 ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////


    // 회원가입 버튼 //
    private void mOnBtnSignupOk(){
        //변수 초기화
        mInitVariables();
        //EditText 값 변수 저장
        mUserId = mEditUserId.getText().toString();
        mUserPwd1 = mEditUserPwd1.getText().toString();
        mUserPwd2 = mEditUserPwd2.getText().toString();
        mUserName = mEditUserName.getText().toString();
        mUserAge = mEditUserAge.getText().toString();
        mUserStature = mEditUserStature.getText().toString();
        mUserWeight = mEditUserWeight.getText().toString();

        //필수 입력 예외 처리
        if(mUserId.equals("") || mUserPwd1.equals("") || mUserPwd2.equals("") || mUserName.equals("") ){
            Toast.makeText(getApplicationContext(), "필수사항을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        //1차 2차 비밀번호 일치 예외처리
        if(!mUserPwd1.equals(mUserPwd2)) {
            Toast.makeText(getApplicationContext(), "입력 비밀번호가 다릅니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        //서버 전송
        //서버 연결 스레드 실행
        ConnServerAsyncTask connServerAsyncTask = new ConnServerAsyncTask();
        connServerAsyncTask.execute(SIGNUP_URL_ADDRESS);
    }


    // 취소 버튼 //
    private void mOnBtnSignupCancel(){
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // 성별 분류 //
    private void mOnRadioBtn(int id){
        switch(id){
            case R.id.radioMale:
                mUserIsMale = true;
                break;
            case R.id.radioFemale:
                mUserIsMale = false;
                break;
        }
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
                jsonObject.accumulate("UserPwd", mUserPwd1);
                jsonObject.accumulate("UserName", mUserName);
                jsonObject.accumulate("UserIsMale", mUserIsMale);
                jsonObject.accumulate("UserAge", mUserAge);
                jsonObject.accumulate("UserStature", mUserStature);
                jsonObject.accumulate("UserWeight", mUserWeight);

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
            if(mServerMsg.equals("ack")){
                Toast.makeText(getApplicationContext(), "회원가입 완료.\n로그인 해주세요.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "이미 존재하는 아이디 입니다.", Toast.LENGTH_SHORT).show();
            }
        }

    }



}
