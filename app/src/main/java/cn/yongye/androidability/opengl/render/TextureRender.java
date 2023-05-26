package cn.yongye.androidability.opengl.render;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.common.ViewUtils;
import cn.yongye.androidability.opengl.activity.TextureActivity;

public class TextureRender implements GLSurfaceView.Renderer {

    private static final String TAG = TextureRender.class.getSimpleName();
    int glSurfaceViewWidth;
    int glSurfaceViewHeight;
    int programId;
    //定点着色器
    private String vertexShaderCode =
            "precision mediump float;\n" +
                    "attribute vec4 a_position;\n" +
                    "attribute vec2 a_textureCoordinate;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "void main() {\n" +
                    "    v_textureCoordinate = a_textureCoordinate;\n" +
                    "    gl_Position = a_position;\n" +
                    "}";

    private String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(u_texture, v_textureCoordinate);\n" +
                    "}";
    // 顶点坐标
    private float[] vertexData = new float[]{-1f, -1f, -1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f};
    private int VERTEX_COMPONENT_COUNT = 2;
    private FloatBuffer vertexDataBuffer;

    // 纹理坐标
    private float[]  textureCoordinateData = new float[]{0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 1f, 0f};
    private int TEXTURE_COORDINATE_COMPONENT_COUNT = 3;
    private FloatBuffer textureCoordinateDataBuffer;

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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexData.length / VERTEX_COMPONENT_COUNT);
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
        // 将三角形顶点数据放入buffer中
        vertexDataBuffer = ByteBuffer.allocateDirect(vertexData.length * Float.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(vertexData);
        vertexDataBuffer.position(0);
        // 获取字段a_position在shader中的位置
        int location = GLES20.glGetAttribLocation(programId, "a_position");
        // 启动对应位置的参数
        GLES20.glEnableVertexAttribArray(location);
        // 指定a_position所使用的顶点数据
        GLES20.glVertexAttribPointer(location, 2, GLES20.GL_FLOAT, false,0, vertexDataBuffer);

        //创建纹理，绑定图片数据为纹理数据
        createAndBindTexture();

    }

    void createAndBindTexture() {
        // 将纹理坐标数据放入buffer中
        textureCoordinateDataBuffer = ByteBuffer.allocateDirect(textureCoordinateData.length * java.lang.Float.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureCoordinateDataBuffer.put(textureCoordinateData);
        textureCoordinateDataBuffer.position(0);
        // 获取字段a_textureCoordinate在shader中的位置
        int aTextureCoordinateLocation = GLES20.glGetAttribLocation(programId, "a_textureCoordinate");
        // 启动对应位置的参数，这里直接使用LOCATION_ATTRIBUTE_TEXTURE_COORDINATE，而无需像OpenGL 2.0那样需要先获取参数的location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation);
        // 指定a_textureCoordinate所使用的顶点数据
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, TEXTURE_COORDINATE_COMPONENT_COUNT
                , GLES20.GL_FLOAT, false,0, textureCoordinateDataBuffer);

        // 创建图片纹理
        // Create texture
        int[] textures = new int[]{0};
        GLES20.glGenTextures(textures.length, textures, 0);
        int imageTexture = textures[0];

        // 设置纹理参数
        // Set texture parameters
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // 解码图片并加载到纹理中
        // Decode the image and load it into texture
        Bitmap bitmap = ViewUtils.getResourceBitmap(TextureActivity.textureActivity, "texture.jpg");
        ByteBuffer b = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(b);
        b.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth()
                , bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, b);
        int uTextureLocation = GLES20.glGetAttribLocation(programId, "u_texture");
        GLES20.glUniform1i(uTextureLocation, 0);
    }
}
