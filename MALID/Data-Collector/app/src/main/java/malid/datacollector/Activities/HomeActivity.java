package malid.datacollector.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class HomeActivity extends AppCompatActivity {

    private static final String TAG="U-Health-home";
    private static final String HOME_URL_ADDRESS="http://13.125.151.92:9000/home";

    private Button mBtnGoList, mBtnGoMain, mBtnLogout, mBtnExit;

    private BackPressCloseHandler backPressCloseHandler;

    private int mUserSessionId;
    private String mServerMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.v(TAG, "homeActivity onCreate");
        //액션 바 숨김
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        backPressCloseHandler = new BackPressCloseHandler(this);

        mBtnGoList = (Button)findViewById(R.id.btnGoListAct);
        mBtnGoMain = (Button)findViewById(R.id.btnGoMainAct);
        mBtnLogout = (Button)findViewById(R.id.btnLogout);
        mBtnExit = (Button)findViewById(R.id.btnExit);

        mUserSessionId = Integer.parseInt(SaveSharedPreference.getUserName(HomeActivity.this));


        //기록 이동
        mBtnGoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                intent.putExtra("sid", mUserSessionId);
                startActivity(intent);
            }
        });




        //메인 이동
        mBtnGoMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("sid", mUserSessionId);
                startActivity(intent);
            }
        });




        //로그아웃
        mBtnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveSharedPreference.clearUserName(HomeActivity.this);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });


        //종료 버튼
        mBtnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //NavUtils.navigateUpFromSameTask(HomeActivity.this);
                finishAffinity();
                System.runFinalizersOnExit(true);
                System.exit(0);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //서버 전송
        //서버 연결 스레드(유저 데이터 update) 실행
        ConnServerAsyncTask connServerAsyncTask = new ConnServerAsyncTask();
        connServerAsyncTask.execute(HOME_URL_ADDRESS);
    }

    // 뒤로가기버튼 리스너
    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 서버 연결 스레드 ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    public class ConnServerAsyncTask extends AsyncTask<String, String, String> {

        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            mServerMsg=null;
            super.onPreExecute();
            pDialog = new ProgressDialog(HomeActivity.this);
            pDialog.setMessage(" loading ...");
            pDialog.show();
            mServerMsg=null;
        }

        @Override
        protected String doInBackground(String... urls) {
            try {

                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("sid", mUserSessionId);

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
                    Log.v(TAG, "error[1] MalformedURLException");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.v(TAG, "error[2] IOException");
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
                        Log.v(TAG, "error[3] IOException");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "error[4] Exception");
                e.printStackTrace();
            }
            return null;
        }



        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(mServerMsg.equals("ack")){
                Toast.makeText(getApplicationContext(), "user setting complete", Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            } else if(result==null) {
                Log.v(TAG, "2");
                Toast.makeText(getApplicationContext(), "인터넷 또는 서버 연결 에러", Toast.LENGTH_SHORT).show();
                SaveSharedPreference.clearUserName(HomeActivity.this);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), mServerMsg, Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            }
        }

    }
}
