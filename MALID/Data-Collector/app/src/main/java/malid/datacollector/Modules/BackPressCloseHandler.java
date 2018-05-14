package malid.datacollector.Modules;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import malid.datacollector.Activities.RootActivity;

public class BackPressCloseHandler {
    private long backKeyPressedTime = 0;
    private Toast toast;

    private Activity activity;

    public  BackPressCloseHandler(Activity context) {
        this.activity = context;
    }

    public void onBackPressed() {
        if(System.currentTimeMillis()>backKeyPressedTime+2000){
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }

        if(System.currentTimeMillis() <= backKeyPressedTime+2000){
            this.activity.finishAffinity();
            System.runFinalizersOnExit(true);
            System.exit(0);
        }


    }

    public void showGuide(){
        toast = Toast.makeText(activity, "\'뒤로\' 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT);
        toast.show();
    }

}
