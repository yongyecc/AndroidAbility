package cn.yongye.handler.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.yongye.handler.R;
import cn.yongye.handler.common.Config;

public class UpdateUIThread extends Activity {

    /**
     * 示例代码1: 动态更新Activity界面上面的数据。3s后，让界面上的数字每秒后增加1.
     */
    public String TAG = this.getClass().getSimpleName();
    TextView textView = null;
    int number = 0;
    Handler uiHandler = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updateui);
        textView = findViewById(R.id.number);
        number = Integer.parseInt(textView.getText().toString());

        //1. Create Handler to update Activity for UI Thread.
        initHandler();

        //2. Send message to UIThread by Handler.
        startAddNumberThread();
    }


    public void initHandler(){
        uiHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, String.format("[uiHandler][handleMessage]Message=%s", msg));

                switch (msg.what){
                    case Config.ADD_ONE:
                        number++;
                        textView.setText(String.valueOf(number));
                        break;
                    default:
                        Log.d(TAG, String.format("[uiHandler][handleMessage]Other what of message %s", msg.what));
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }


    public void startAddNumberThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    while (true) {
                        Message message = Message.obtain();
                        message.what = Config.ADD_ONE;
                        uiHandler.sendMessage(message);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
