package malid.datacollector.Modules;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import malid.datacollector.CounterService;

public class MyCounterService extends Service {
    public MyCounterService() {
    }

    private int count;

    CounterService.Stub binder = new CounterService.Stub() {
        @Override
        public int getCount() throws RemoteException {
            return count;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Thread counter = new Thread(new Counter());
        counter.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isStop = true;
        return super.onUnbind(intent);
    }

    private boolean isStop;

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStop = true;
    }

    private class Counter implements Runnable {
        private Handler handler = new Handler();

        @Override
        public void run() {

            count = 0;
            while ( true ) {

                if ( isStop ) {
                    break;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COUNT", count + "");
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count ++;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("COUNT",  "Count end");
                }
            });
        }
    }

}
