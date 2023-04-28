package cn.yongye.androidability.hook.preview;

import java.util.LinkedList;

import de.robv.android.xposed.DexposedBridge;

public class HookPreviewManager {

    public static LinkedList<byte[]> mYUVQueue = new LinkedList<>();    //存储预览帧数据的队列
    public static boolean printSize = false;      //打印宽高
    public static boolean startRecord = false;    //是否开始mediacodec解码YUV并转存MP4.
    public static int frameIndex = -1;

    static HookPreviewManager previewManager;

    //存放预览数据的文件目录
    private String CacheDirectoryPath = null;

    public String getCacheDirectoryPath() {
        return CacheDirectoryPath;
    }

    public void setCacheDirectoryPath(String cacheDirectoryPath) {
        CacheDirectoryPath = cacheDirectoryPath;
    }

    public static HookPreviewManager getInstance() {
        if (previewManager == null) {
            previewManager = new HookPreviewManager();
        }
        return previewManager;
    }


    public LinkedList getYUVQueue() {
        DexposedBridge.log("mYUVQueue.size=" + mYUVQueue.size());
        int sleepCount = 0;
        if (mYUVQueue.isEmpty()) {
            //等待1s
            while (sleepCount < 1000) {
                if (!mYUVQueue.isEmpty()) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (mYUVQueue.isEmpty()) {
                DexposedBridge.log("mYUVQueue empty, close muxer");
                EncoderManager.getInstance().close();
            }
        }
        return mYUVQueue;
    }

    /**
     * 本地预览结束时,重置部分状态属性.
     */
    public void resetStatusOnFinishPreview() {
        printSize = false;
        startRecord = false;
        //对方画面帧索引
        frameIndex = -1;
    }
}
