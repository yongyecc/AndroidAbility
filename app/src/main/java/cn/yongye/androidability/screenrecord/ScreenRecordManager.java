package cn.yongye.androidability.screenrecord;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import cn.yongye.androidability.screenrecord.service.ScreenRecordService;

/**
 * 屏幕共享管理类
 */
public class ScreenRecordManager {


    private static ScreenRecordManager mInstance;
    private MediaProjectionManager mMediaProjectionManager;

    /**
     * 返回DataChannelManager对象.
     */
    public static ScreenRecordManager getInstance() {
        if (mInstance == null) {
            synchronized (ScreenRecordManager.class) {
                if (mInstance == null) {
                    mInstance = new ScreenRecordManager();
                }
            }

        }
        return mInstance;
    }

    private ScreenRecordManager() {
    }

    /**
     * 开始录屏.
     */
    public void startScreenRecord(Activity activity) {
        if (mMediaProjectionManager == null) {
            mMediaProjectionManager = getMediaProjectionManager(activity);
        }
        Intent screenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(screenCaptureIntent, ScreenRecordService.REQUEST_SCREEN_RECORDER_CODE);
    }

    public MediaProjectionManager getMediaProjectionManager(Context context) {
        if (mMediaProjectionManager == null) {
            mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
        }
        return mMediaProjectionManager;
    }

    /**
     * 结束共享屏幕
     */
    public void stopScreenRecord(Context context) {
        ScreenRecordBean.RECORD_STATUS = false;
        context.stopService(new Intent(context, ScreenRecordService.class));
    }
}