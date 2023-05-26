package cn.yongye.androidability.opengl.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.yongye.androidability.common.LogUtil;

/**
 * 三角形着色器.
 * From: https://juejin.cn/post/6844903798599581704
 */
public class TriangleRender implements GLSurfaceView.Renderer {

    private static final String TAG = TriangleRender.class.getSimpleName();
    int glSurfaceViewWidth;
    int glSurfaceViewHeight;
    int programId;
    //定点着色器
    private String vertexShaderCode =
            "precision mediump float;\n" +
                    "attribute vec4 a_Position;\n" +
                    "uniform float u_Ratio;\n" +
                    "uniform float u_Rotate;\n" +
                    "void main() {\n" +
                    "   vec4 p = a_Position;\n" +
                    "   p.y = p.y / u_Ratio;\n" +
                    "   mat4 rotateMatrix = mat4(cos(u_Rotate), sin(u_Rotate), 0.0, 0.0,\n" +
                    "                         -sin(u_Rotate), cos(u_Rotate), 0.0, 0.0,\n" +
                    "                         0.0, 0.0, 1.0, 0.0,\n" +
                    "                         0.0, 0.0, 0.0, 1.0);\n" +
                    "    p = rotateMatrix * p;\n" +
                    "    p.y = p.y * u_Ratio;\n" +
                    "    gl_Position = p;\n" +
                    "}";
    //片段着色器
    private String fragmentShaderCode =
            "precision mediump float;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);\n" +
                    "}";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtil.i(TAG, String.format("[onSurfaceCreated]"));
        CreateGLPogram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtil.i(TAG, String.format("[onSurfaceChanged] %sx%s", width, height));
        // 记录GLSurfaceView的宽高
        glSurfaceViewWidth = width;
        glSurfaceViewHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        LogUtil.i(TAG, String.format("[onDrawFrame]"));
        // 应用GL程序
        GLES20.glUseProgram(programId);

        prepareVertexData();

        prepareView();

        // 调用draw方法用TRIANGLES的方式执行渲染，顶点数量为3个
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    void CreateGLPogram() {
        //创建OpenGL程序
        programId = GLES20.glCreateProgram();
        // 加载、编译vertex shader和fragment shader
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader= GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(vertexShader);
        GLES20.glCompileShader(fragmentShader);
        // 将shader程序附着到GL程序上
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        // 链接GL程序
        GLES20.glLinkProgram(programId);
    }

    private void prepareView() {
        // 设置清屏颜色
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1f);
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // 设置视口，这里设置为整个GLSurfaceView区域
        GLES20.glViewport(0, 0, glSurfaceViewWidth, glSurfaceViewHeight);
    }

    void prepareVertexData() {
        // 三角形顶点数据
        float[] vertexData = new float[]{0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f};
        // 将三角形顶点数据放入buffer中
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * Float.SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(vertexData);
        buffer.position(0);
        // 获取字段a_Position在shader中的位置
        int location = GLES20.glGetAttribLocation(programId, "a_Position");
        // 启动对应位置的参数
        GLES20.glEnableVertexAttribArray(location);
        // 指定a_Position所使用的顶点数据
        GLES20.glVertexAttribPointer(location, 2, GLES20.GL_FLOAT, false,0, buffer);

        //u_Ratio修复旋转导致图形变形问题.
        // 获取字段u_Ratio在shader中的位置
        int uRatioLocation = GLES20.glGetUniformLocation(programId, "u_Ratio");
        // 启动对应位置的参数
        GLES20.glEnableVertexAttribArray(uRatioLocation);
        // 指定u_Ratio所使用的顶点数据
        GLES20.glUniform1f(uRatioLocation, glSurfaceViewWidth * 1.0f / glSurfaceViewHeight);

        //设置旋转角度
        int uRotateLocation = GLES20.glGetUniformLocation(programId, "u_Rotate");
        GLES20.glEnableVertexAttribArray(uRotateLocation);
        GLES20.glUniform1f(uRotateLocation, (float) Math.toRadians(90));
    }
}
