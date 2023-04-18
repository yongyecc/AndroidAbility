package cn.yongye.androidability.activitytask;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;

/**
 * 新增一个task放入activity.
 */
public class SingleInstanceActivity extends Activity {

    TextView textView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_page);

        textView = findViewById(R.id.show_text);
        textView.setText(SingleInstanceActivity.class.getSimpleName());
    }
}