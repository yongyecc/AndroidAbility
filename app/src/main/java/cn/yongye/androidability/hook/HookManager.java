package cn.yongye.androidability.hook;

import android.content.Context;
import android.media.MediaCodec;

import java.nio.ByteBuffer;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Hook管理模块.
 * 使用相关库: https://github.com/tiann/epic
 */
public class HookManager {

    private static HookManager hookManager;

    //单例.
    public static HookManager getInstance() {
        if (hookManager == null) {
            hookManager = new HookManager();
        }
        return hookManager;
    }

    /**
     * hook 视频编解码组件mediacodec.
     */
    public void hookMediaCodec(Context context) {

        Class recordThreadClazz = null;
        try {
            recordThreadClazz = Class.forName("cn.yongye.androidability.screenrecord.MediaMuxerScreenRecordThread"
                    , true, context.getClassLoader());
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        DexposedBridge.findAndHookMethod(recordThreadClazz, "onEncodedAvcFrame", ByteBuffer.class
                , MediaCodec.BufferInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        ByteBuffer byteBuffer = (ByteBuffer) param.args[0];
                        MediaCodec.BufferInfo bufferInfo = (MediaCodec.BufferInfo) param.args[1];
                        DexposedBridge.log(String.format("[*] bytebuffer=%s bufferinfo=%s", byteBuffer
                                , bufferInfo));
                    }
                });
    }
}
