package cn.yongye.handler.demo.bindview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import butterknife.BindView;
import cn.yongye.handler.R;

public class BindViewActivity extends Activity {

    @BindView(R.id.bindview_title)
    TextView title;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bindview);
    }
}
