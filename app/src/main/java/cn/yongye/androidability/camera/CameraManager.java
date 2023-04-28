package cn.yongye.androidability.camera;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import java.io.IOException;

import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.common.PermissionUtil;

/**
 * 管理使用Camera功能.
 */
public class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();
    static CameraManager cameraManager;
    Camera mCamera;
    private SurfaceTexture mPreviewSurface; //预览surface

    /**
     * 单例.
     * @return .
     */
    public static CameraManager getInstance() {
        if (cameraManager == null) {
            cameraManager = new CameraManager();
        }
        return cameraManager;
    }

    /**
     * 打开相机.
     */
    public void openCamera(Activity context, boolean openFrontCamera) {
        if (PermissionUtil.checkPermission(context, Manifest.permission.CAMERA)) {
            if (openFrontCamera) {
                //打开前置摄像头
                CameraBean.OPEN_FRONT_CAMERA = true;
                mCamera = Camera.open(getCameraId(true));
            } else {
                //打开后置摄像头
                CameraBean.OPEN_FRONT_CAMERA = false;
                mCamera = Camera.open(getCameraId(false));
            }
            //矫正预览方向
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(getCameraId(CameraBean.OPEN_FRONT_CAMERA), cameraInfo);
            mCamera.setDisplayOrientation(getCameraDisplayOrientation(context, cameraInfo));
            LogUtil.i(TAG, "[*] Open Camera, is front camera: " + CameraBean.OPEN_FRONT_CAMERA);
            // 设置相机方向，后面2.1处详细讲述
            //mCamera.setDisplayOrientation(getCameraDisplayOrientation(mCameraInfo));
        } else {
            PermissionUtil.checkAndRequestMorePermissions(context, new String[] {Manifest.permission.CAMERA}
                    , PermissionUtil.CODE_REQUEST_CAMERA_PERMISSION);
        }
    }

    /**
     * 开始预览.
     * @param previewSurface
     */
    public void startPreview(SurfaceTexture previewSurface) {
        if (mCamera != null && previewSurface != null) {
            try {
                mPreviewSurface = previewSurface;
                mCamera.setPreviewTexture(previewSurface);
                mCamera.startPreview();
                CameraBean.PREVIEWING = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 结束预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            CameraBean.PREVIEWING = false;
            LogUtil.i(TAG, "[*] stopPreview.");
        } else {
            LogUtil.e(TAG, "[Error] stopPreview failed, mCamera=" + mCamera);
        }
    }

    /**
     * 关闭相机.
     */
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            LogUtil.i(TAG, "[*] Close Camera.");
        }
    }

    /**
     * 获取Camera id.
     * @param isFrontCamera 是否获取前置摄像头,否则获取后置摄像头.
     * @return  .
     */
    int getCameraId(boolean isFrontCamera) {
        int cameraId = -1;
        Camera.CameraInfo cameraInfo = null;
        int cameraSize = Camera.getNumberOfCameras();
        for (int cid = 0; cid < cameraSize; cid++) {
            cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cid, cameraInfo);
            switch (cameraInfo.facing) {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    LogUtil.d(TAG, "[Camera] Front cameraId=" + cid);
                    if (isFrontCamera) {
                        cameraId = cid;
                    }
                    break;
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    LogUtil.d(TAG, "[Camera] Back cameraId=" + cid);
                    if (!isFrontCamera) {
                        cameraId = cid;
                    }
                    break;
                default:
                    LogUtil.e(TAG, "[Failed] Not front or back camera, facing=" + cameraInfo.facing);
                    break;
            }
        }
        return cameraId;
    }

    //预览方向矫正.
    private int getCameraDisplayOrientation(Activity context, Camera.CameraInfo cameraInfo) {
        int roration = context.getWindowManager().getDefaultDisplay().getRotation();
        // 屏幕显示方向角度(相对局部坐标Y轴正方向夹角)
        int degrees = 0;
        switch (roration) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        // 相机需要校正的角度
        return result;
    }
}
