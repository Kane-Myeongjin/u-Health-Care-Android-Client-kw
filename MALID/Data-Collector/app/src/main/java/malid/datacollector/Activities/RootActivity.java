package malid.datacollector.Activities;

import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import malid.datacollector.Modules.SaveSharedPreference;
import malid.datacollector.R;

public class RootActivity extends AppCompatActivity {

    private static final String TAG="U-Health-root";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);
        Log.v(TAG, "rootActivity onCreate");

        // 자동 로그인
        if(SaveSharedPreference.getUserName(RootActivity.this).length() == 0)
        {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }
        else
        {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
        }
    }
}