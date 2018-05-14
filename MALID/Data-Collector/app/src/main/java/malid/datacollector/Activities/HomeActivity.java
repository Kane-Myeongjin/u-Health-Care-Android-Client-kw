package malid.datacollector.Activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import malid.datacollector.Modules.BackPressCloseHandler;
import malid.datacollector.Modules.SaveSharedPreference;
import malid.datacollector.R;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG="U-Health-home";

    private Button mBtnGoList, mBtnGoMain, mBtnLogout, mBtnExit;

    private BackPressCloseHandler backPressCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //액션 바 숨김
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        backPressCloseHandler = new BackPressCloseHandler(this);

        mBtnGoList = (Button)findViewById(R.id.btnGoListAct);
        mBtnGoMain = (Button)findViewById(R.id.btnGoMainAct);
        mBtnLogout = (Button)findViewById(R.id.btnLogout);
        mBtnExit = (Button)findViewById(R.id.btnExit);



        //기록 이동
        mBtnGoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
                startActivity(intent);
            }
        });




        //메인 이동
        mBtnGoMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
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
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }
}
