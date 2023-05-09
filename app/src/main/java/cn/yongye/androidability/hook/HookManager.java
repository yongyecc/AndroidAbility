package cn.yongye.androidability.hook;

import android.content.Context;
import android.hardware.Camera;

import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.hook.preview.EncoderManager;
import cn.yongye.androidability.hook.preview.MediaMuxerManager;
import cn.yongye.androidability.hook.preview.HookPreviewManager;
import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook管理模块.
 * 使用相关库: https://github.com/tiann/epic
 */
public class HookManager {

    private static final String TAG = HookManager.class.getSimpleName();
    private static HookManager hookManager;

    //单例.
    public static HookManager getInstance() {
        if (hookManager == null) {
            hookManager = new HookManager();
        }
        return hookManager;
    }

    /**
     * Hook Camera的预览方法,转存预览画面.
     */

    public void hookCameraPreview(Context context) {
        DexposedBridge.hookAllMethods(Camera.class, "startPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                LogUtil.i(TAG, "Camera.startPreview called");
                Camera camera1 = (Camera) param.thisObject;
                LogUtil.printStact(TAG);
                //设置回调方法,获取预览数据.
                camera1.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if (HookPreviewManager.mYUVQueue.size() > 100) {
                            HookPreviewManager.mYUVQueue.poll();
                        }
                        HookPreviewManager.mYUVQueue.add(data);
                        if (!HookPreviewManager.printSize) {
                            DexposedBridge.log(String.format("%sx%s", camera.getParameters().getPreviewSize().height
                                    , camera.getParameters().getPreviewSize().width));
                            HookPreviewManager.printSize = true;
                        }
                        if (!HookPreviewManager.startRecord) {
                            DexposedBridge.log("start record preview mp4.");
                            HookPreviewManager.getInstance().setCacheDirectoryPath(context.getCacheDir().getAbsolutePath());
                            MediaMuxerManager.getInstance().init();
                            EncoderManager.getInstance().startEncode(camera.getParameters().getPreviewSize());
                            HookPreviewManager.startRecord = true;
                        }
                    }
                });
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });

        DexposedBridge.hookAllMethods(Camera.class, "_stopPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                DexposedBridge.log("Camera.stopPreview called");
                DexposedBridge.log("mYUVQueue empty, close muxer");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!HookPreviewManager.mYUVQueue.isEmpty()) {
                            try {
                                Thread.sleep(1000);
                            }catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        EncoderManager.getInstance().close();
                        HookPreviewManager.getInstance().resetStatusOnFinishPreview();
                    }
                }).start();
                Camera camera = (Camera) param.thisObject;
                camera.setPreviewCallback(null);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
    }
}
