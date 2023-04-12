package cn.yongye.androidability.dooddle.activity;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;
import cn.yongye.androidability.dooddle.view.PaintView;

public class PathActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);
        findViewById(R.id.paint_view);

    }
}
