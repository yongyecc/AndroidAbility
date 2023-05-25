package cn.yongye.androidability.opengl.activity;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;
import cn.yongye.androidability.opengl.TriangleRender;

public class TriangleActivity extends Activity {


    GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triangle);

        initView();
    }

    private void initView() {
        glSurfaceView = findViewById(R.id.triangleGLSurfaceView);
        // 设置RGBA颜色缓冲、深度缓冲及stencil缓冲大小
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        // 设置GL版本，这里设置为2.0
        glSurfaceView.setEGLContextClientVersion(2);
        //设置着色器
        glSurfaceView.setRenderer(new TriangleRender());
    }
}
