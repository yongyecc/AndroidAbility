package cn.yongye.androidability.screenrecord;

public class ScreenRecordBean {

    //true: 正在录制； false: 未录制
    public static boolean RECORD_STATUS = false;
    //1: MeidaRecord录制; 2:MediaMuxer+mediacodec录制
    public static int SCREEN_RECORD_TYPE = 0;
    public static int RECORD_TYPE_MEDIARECORD = 1;
    public static int RECORD_TYPE_MEDIAMUXER = 2;

}
