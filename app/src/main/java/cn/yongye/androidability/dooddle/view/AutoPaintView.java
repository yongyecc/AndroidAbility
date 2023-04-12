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
 * 自动涂鸦的画板View.
 * From https://blog.csdn.net/legend12300/article/details/51122314.
 */
public class AutoPaintView extends View {

    Canvas mCanvas;
    Paint mPaint;
    Path mPath;

    public AutoPaintView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public AutoPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoPaintView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        // 设置画笔宽度
        mPaint.setStrokeWidth(3);
        // 消除锯齿
        mPaint.setAntiAlias(true);
        // 设置镂空（方便查看效果）
        mPaint.setStyle(Style.STROKE);
        mCanvas = new Canvas();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mPath == null) {
            return;
        }
        //这里只能用参数里的画板对象
        canvas.drawPath(mPath, mPaint);
    }

    /**
     * 绘制接口，提供对外访问的画板绘制接口.
     */
    public void drawLine(Path path) {
        if (mCanvas == null) {
            return;
        }
        mPath = path;
        draw(mCanvas);
    }
}
