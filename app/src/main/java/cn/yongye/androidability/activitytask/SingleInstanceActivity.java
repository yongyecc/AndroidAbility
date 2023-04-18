package cn.yongye.androidability.activitytask;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;

public class SingleInstanceActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
    }
}