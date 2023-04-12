package cn.yongye.androidability.dooddle.view;

import android.content.Context;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;


/**
 * Path涂鸦的自定义View.
 * From https://blog.csdn.net/legend12300/article/details/51122314.
 */
public class AutoPathView extends View {

    private Paint paint;

    public AutoPathView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public AutoPathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoPathView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {

        paint = new Paint();
        paint.setColor(Color.RED);
        // 设置画笔宽度
        paint.setStrokeWidth(3);
        // 消除锯齿
        paint.setAntiAlias(true);
        // 设置镂空（方便查看效果）
        paint.setStyle(Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        //绘制第一个线条
        Path path = new Path();
        path.lineTo(100,100);
        path.lineTo(100, 200);
        path.lineTo(150, 250);
        canvas.drawPath(path, paint);

        //将第一个线条在X轴横移100，绘制出第二个线条
        path.addPath(path, 100, 0);
        canvas.drawPath(path, paint);
    }
}
