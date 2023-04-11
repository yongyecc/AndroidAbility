package cn.yongye.androidability.screenrecord.service;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.yongye.androidability.R;
import cn.yongye.androidability.activity.MainActivity;
import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.common.ViewUtils;
import cn.yongye.androidability.screenrecord.MediaMuxerScreenRecordThread;
import cn.yongye.androidability.screenrecord.ScreenRecordBean;
import cn.yongye.androidability.screenrecord.ScreenRecordManager;

/**
 * 基于MediaRecorder的屏幕录制Service.
 */
public class ScreenRecordService extends Service {
    private final String TAG = ScreenRecordService.class.getSimpleName();
    public static final int REQUEST_SCREEN_RECORDER_PERMISSION = 0;
    public static final int REQUEST_SCREEN_RECORDER_CODE = 1;
    private static final int REQUEST_SCREEN_RECORD_NOTIFICATION_CODE = 11;
    private int mResultCode;
    private Intent mResultData;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;
    MediaMuxerScreenRecordThread muxerScreenRecordThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mResultCode = intent.getIntExtra("resultCode", -1);
        mResultData = intent.getParcelableExtra("intent");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNotificationChannel();
            mMediaProjection = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                    .getMediaProjection(mResultCode, mResultData);
            switch (ScreenRecordBean.SCREEN_RECORD_TYPE) {
                case ScreenRecordBean.RECORD_TYPE_MEDIARECORD:
                    //MediaRecoder录屏
                    mMediaRecorder = createMediaRecorder();
                    mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG,
                            ScreenRecordBean.mScreenWidth, ScreenRecordBean.mScreenHeight,
                            ScreenRecordBean.mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            mMediaRecorder.getSurface(), null, null);
                    mMediaRecorder.start();

                    break;
                case ScreenRecordBean.RECORD_TYPE_MEDIAMUXER:
                    //MediaMuxer+mediacodec录屏
                    muxerScreenRecordThread = new MediaMuxerScreenRecordThread(ScreenRecordBean.mScreenWidth,
                            ScreenRecordBean.mScreenHeight, ScreenRecordBean.mScreenWidth *
                            ScreenRecordBean.mScreenHeight * 6, 1, mMediaProjection,
                            getApplicationContext().getFilesDir().getAbsolutePath()
                                    + File.separator + "muxer_record.mp4");
                    muxerScreenRecordThread.start();
                    break;
                default:
                    break;
            }
            ScreenRecordBean.RECORD_STATUS = true;

        }
        return Service.START_NOT_STICKY;
    }

    /**
     * 创建状态栏通知，保证高版本录制功能的正常开启.
     */
    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, REQUEST_SCREEN_RECORD_NOTIFICATION_CODE,
                    intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, REQUEST_SCREEN_RECORD_NOTIFICATION_CODE,
                    intent, PendingIntent.FLAG_ONE_SHOT);
        }
        builder.setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(ViewUtils.getStringById(this.getApplicationContext(), R.string.recording))
                .setWhen(System.currentTimeMillis());

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }

    /**
     * 创建MediaRecorder对象.
     *
     * @return .
     */
    private MediaRecorder createMediaRecorder() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate).replace(" ", "");
        String videoQuality = "HD";
        if (ScreenRecordBean.isVideoSd) {
            videoQuality = "SD";
        }
        LogUtil.i(TAG, "Create MediaRecorder");
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + videoQuality + curTime + ".mp4");
        mediaRecorder.setVideoSize(ScreenRecordBean.mScreenWidth, ScreenRecordBean.mScreenHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (ScreenRecordBean.isVideoSd) {
            mediaRecorder.setVideoEncodingBitRate(ScreenRecordBean.mScreenWidth * ScreenRecordBean.mScreenHeight);
            mediaRecorder.setVideoFrameRate(ScreenRecordBean.FRAME_RATE);
        } else {
            mediaRecorder.setVideoEncodingBitRate(5 * ScreenRecordBean.mScreenWidth * ScreenRecordBean.mScreenHeight);
            mediaRecorder.setVideoFrameRate(ScreenRecordBean.FRAME_RATE_60);
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
        return mediaRecorder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.i(TAG, "onDestroy");
        //end MediaMuxer
        if (muxerScreenRecordThread != null) {
            muxerScreenRecordThread.quit();
        }
        if (mVirtualDisplay != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mVirtualDisplay.release();
            }
            mVirtualDisplay = null;
        }
        // end MediaRecoder
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection.stop();
            }
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection.stop();
            }
            mMediaProjection = null;
        }
    }
}
