package cn.yongye.androidability.camera.activity;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;

import cn.yongye.androidability.R;
import cn.yongye.androidability.camera.CameraBean;
import cn.yongye.androidability.camera.CameraManager;
import cn.yongye.androidability.common.ViewUtils;

public class CameraPreviewActivity extends Activity {

    TextureView textureView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        initView();
    }

    private void initView() {
        textureView = findViewById(R.id.preview_surfaceview);

    }

    //onClick: 开始预览.
    //bugfix: onCreate里无法打开预览功能.
    public void openPreview(View view) {
        //默认开启摄像头预览画面,默认前置摄像头.
        CameraManager.getInstance().openCamera(this, true);
        CameraManager.getInstance().startPreview(textureView.getSurfaceTexture());
    }

    //onClick: 结束预览
    public void stopPreview(View view) {
        if (!CameraBean.PREVIEWING) {
            ViewUtils.showToast(this, ViewUtils.getStringById(this, R.string.start_preview_tip));
            return;
        }
        CameraManager.getInstance().stopPreview();
    }

    //onClick: 切换摄像头
    public void switchCamera(View view) {
        if (!CameraBean.PREVIEWING) {
            ViewUtils.showToast(this, ViewUtils.getStringById(this, R.string.start_preview_tip));
            return;
        }
        CameraManager.getInstance().stopPreview();
        CameraManager.getInstance().closeCamera();
        if (CameraBean.OPEN_FRONT_CAMERA) {
            CameraManager.getInstance().openCamera(this, false);
        } else {
            CameraManager.getInstance().openCamera(this, true);
        }
        CameraManager.getInstance().startPreview(textureView.getSurfaceTexture());
    }
}
