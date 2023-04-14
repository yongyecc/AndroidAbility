package cn.yongye.androidability.dooddle.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Paint.Style;



/**
 * 自定义布局：随着绘制指针.
 * From https://blog.csdn.net/qq_38861828/article/details/115010299.
 */
public class AutoMoveRect extends View {

    Canvas mCanvas;
    Paint mPaint;
    Path mPath;

    public AutoMoveRect(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public AutoMoveRect(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoMoveRect(Context context) {
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
}
