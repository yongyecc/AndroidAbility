package cn.yongye.handler.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.yongye.handler.R;
import cn.yongye.handler.common.Config;

public class Demo2ThreadAToThreadB extends Activity {

    /** 线程间通信，通信过程如下:
     *      线程A向线程B发送一个消息，"Hi thread B, this's thread A".
     *      线程B收到消息后，回复线程A一个消息，"Dear thread A, i'am fine, thanks and bye.".
     *      线程A收到消息，通信结束。
     */

    public String TAG = this.getClass().getSimpleName();
    Handler handlerA = null;
    Handler handlerB = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo2);

        //start thread communicate.
        startThread();
    }


    void startThread() {
        //init thread A
        if (handlerA == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Looper.prepare();
                        handlerA = new Handler() {
                            @Override
                            public void handleMessage(@NonNull Message msg) {
                                super.handleMessage(msg);
                                Log.d(TAG, String.format("[ThreadA][UseHandlerA][GetMessage]\t%s", msg.obj));
                            }
                        };
                        // wait for handlerB isn't null
                        Thread.sleep(1000);
                        while (handlerB==null) {
                            ;
                        }
                        Message msg = Message.obtain();
                        msg.what = Config.THREADA;
                        msg.obj = "Hi thread B, this's thread A";
                        Log.d(TAG, String.format("[ThreadA][UseHandlerB][SendMessage]\t%s", msg.obj));
                        handlerB.sendMessage(msg);
                        //enter loop.
                        Looper.loop();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        //init thread B
        if (handlerB == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    handlerB = new Handler() {
                        @Override
                        public void handleMessage(@NonNull Message msg) {
                            super.handleMessage(msg);
                            Log.d(TAG, String.format("[ThreadB][UseHandlerB][GetMessage]\t%s", msg.obj));
                            Message msgB = Message.obtain();
                            msgB.what = Config.THREADA;
                            switch (msg.what) {
                                case Config.THREADA:
                                    msgB.obj = "Dear thread A, i'am fine, thanks and bye.";
                                    Log.d(TAG, String.format("[ThreadB][UseHandlerA][SendMessage]\t%s", msgB.obj));
                                    handlerA.sendMessage(msgB);
                                    break;
                            }
                        }
                    };
                    Looper.loop();
                }
            }).start();
        }
    }
}
