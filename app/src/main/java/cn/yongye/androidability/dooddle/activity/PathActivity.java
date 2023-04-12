package cn.yongye.androidability.dooddle.activity;

import android.app.Activity;
import android.graphics.Path;
import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;
import cn.yongye.androidability.dooddle.view.AutoPaintView;

public class PathActivity extends Activity {

    public static AutoPaintView autoPaintView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path);
        findViewById(R.id.paint_view);

        autoPaintView = findViewById(R.id.auto_paint);

        //绘制第一个线条
        Path path = new Path();
        path.lineTo(100,100);
        path.lineTo(100, 200);
        path.lineTo(150, 250);
        path.addPath(path, 100, 0); //将第一个线条在X轴横移100，绘制出第二个线条
        autoPaintView.drawLine(path);
    }
}
