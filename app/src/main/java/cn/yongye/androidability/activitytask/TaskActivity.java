package cn.yongye.androidability.activitytask;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;

/**
 * 同一个task里新增的activity，可以通过back键返回task队列里的上一个activity.
 */
public class TaskActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
    }
}
