package malid.datacollector.Activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
    private static final String HIST_URL_ADDRESS="http://13.125.151.92:9000/hist";

    private String mServerMsg;
    private int mUserSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setTitle("History");

        Intent intent = getIntent();
        mUserSessionId = intent.getExtras().getInt("sid");
        Toast.makeText(getApplicationContext(), "auth sid:"+Integer.toString(mUserSessionId), Toast.LENGTH_LONG).show();

        //뒤로가기 버튼 추가
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        //서버에 히스토리 요청
        ConnServerAsyncTask connServerAsyncTask = new ConnServerAsyncTask();
        connServerAsyncTask.execute(HIST_URL_ADDRESS);
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
                Toast.makeText(getApplicationContext(), "ack", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "nack", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
