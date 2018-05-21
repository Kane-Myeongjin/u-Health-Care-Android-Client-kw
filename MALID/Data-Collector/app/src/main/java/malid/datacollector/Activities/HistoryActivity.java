package malid.datacollector.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
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

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG="U-Health-history";
    private static final String HIST_URL_ADDRESS="http://13.125.151.92:9000/static/history/";

    private String mServerMsg;
    private int mUserSessionId;

    private Bitmap mBitmap;

    private ImageView mImgHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Log.v(TAG, "historyActivity onCreate");
        setTitle("History");

        Intent intent = getIntent();
        mUserSessionId = intent.getExtras().getInt("sid");
        //Toast.makeText(getApplicationContext(), "auth sid:"+Integer.toString(mUserSessionId), Toast.LENGTH_LONG).show();

        //뒤로가기 버튼 추가
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mImgHistory = (ImageView) findViewById(R.id.imgViewHist);


        //서버에 히스토리 요청
        GetHistory getHistory = new GetHistory();
        getHistory.execute(HIST_URL_ADDRESS+Integer.toString(mUserSessionId)+".jpg");
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
        finish();
    }

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// 서버 연결 스레드 ////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    private class GetHistory extends AsyncTask<String, String, Bitmap> {

        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(HistoryActivity.this);
            pDialog.setMessage("history 불러오는 중");
            pDialog.show();
        }

        protected Bitmap doInBackground(String... args) {
            try {
                mBitmap = BitmapFactory
                        .decodeStream((InputStream) new URL(args[0])
                                .getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return mBitmap;
        }

        protected void onPostExecute(Bitmap image) {

            if (image != null) {
                mImgHistory.setImageBitmap(image);
                pDialog.dismiss();
            } else {
                pDialog.dismiss();
                Toast.makeText(HistoryActivity.this, "history 가 존재하지 않습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}
