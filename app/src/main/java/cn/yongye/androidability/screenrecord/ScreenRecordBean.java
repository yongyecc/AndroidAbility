package cn.yongye.androidability.screenrecord;

import android.Manifest;

public class ScreenRecordBean {

    //true: 正在录制； false: 未录制
    public static boolean RECORD_STATUS = false;
    //1: MeidaRecord录制; 2:MediaMuxer+mediacodec录制
    public static int SCREEN_RECORD_TYPE = 0;
    public final static int RECORD_TYPE_MEDIARECORD = 1;
    public final static int RECORD_TYPE_MEDIAMUXER = 2;
    public static int mScreenWidth = 1080;  //录屏分辨率宽度
    public static int mScreenHeight = 2160; //录屏分辨率高度
    public static final int FRAME_RATE = 30; //帧率
    public static final int FRAME_RATE_60 = 60;
    public static final int IFRAME_INTERVAL = 1; //两个关键帧的时间间隔，单位秒
    public static  int mScreenDensity = 1;
    //录屏所需权限，申请权限需要在AndroidMainfest.xml中声明
    public static String[] screenPermission = new String[]{Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static boolean isVideoSd = false;    //是否为标清视频
}
