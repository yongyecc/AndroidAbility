package cn.yongye.androidability.dooddle.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Path;
import android.graphics.PorterDuff;

import androidx.annotation.Nullable;

import cn.yongye.androidability.dooddle.activity.PathActivity;


/**
 * 手动涂鸦的画板View.
 */
public class ManualPaintView extends View {


    private Paint mPaint;
    private Path mPath;
    private Canvas mCanvas;
    private float currentX, currentY;


    public ManualPaintView(Context context) {
        super(context);
        init();
    }

    //bugfix: Caused by: java.lang.NoSuchMethodException: cn.yongye.androidability.dooddle.view.ManualPaintView.<init> [class android.content.Context, interface android.util.AttributeSet]
    public ManualPaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 初始化画板.
     */
    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true); // 去除锯齿
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPath = new Path();
        mCanvas = new Canvas();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentX = x;
                currentY = y;
                mPath.moveTo(currentX, currentY);
                break;
            case MotionEvent.ACTION_MOVE:
                currentX = x;
                currentY = y;
                mPath.lineTo(currentX, currentY);
                //mPath.quadTo(currentX, currentY, x, y); // 画线
                //在手动绘制画板绘制时，同步更新绘制内容到自动绘制画板上.
                PathActivity.autoPaintView.drawLine(mPath);
                PathActivity.autoPaintView.invalidate(); //更新UI界面
                break;
            case MotionEvent.ACTION_UP:
                mCanvas.drawPath(mPath, mPaint);
                break;
        }
        invalidate();
        return true;
    }

    public Path getPath() {
        return mPath;
    }

    //清除画板
    public void clear() {
        if (mCanvas != null) {
            mPath.reset();
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            invalidate();
        }
    }
}
