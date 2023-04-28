package cn.yongye.androidability.hook.preview;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import de.robv.android.xposed.DexposedBridge;


/**
 * Created by zhangxd on 2018/7/3.
 * 编码，支持同步和异步方式
 * 生成码流与MP4文件
 */

public class EncoderManager {

    private static final String TAG = EncoderManager.class.getSimpleName();

    public static String PATH = HookPreviewManager.getInstance().getCacheDirectoryPath() + "/localPreview.h264";

    private BufferedOutputStream outputStream;

    private MediaCodec mediaCodec;

    private long frameIndex;

    private static EncoderManager instance;

    private volatile boolean isMuxFinish = false;

    private int mTrackIndex;

    private Thread mEncodeThread;

    private int mWidth;

    private int mHeight;

    private byte[] yuv420spsrc;

    public EncoderManager() {
        init();
    }

    public static EncoderManager getInstance() {
        if (instance == null) {
            instance = new EncoderManager();
        }
        return instance;
    }

    public void setMuxFinish(boolean muxFinish) {
        isMuxFinish = muxFinish;
    }

    private void createfile() {
        File file = new File(PATH);
        if (!file.exists()) {
            file.getParentFile().mkdir();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void init() {
        createfile();
    }


    public void initMediaCodec(Camera.Size size) {
        mWidth = size.width;
        mHeight = size.height;
        yuv420spsrc = new byte[mWidth * mHeight * 3 / 2];
        //编码格式，AVC对应的是H264
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        //YUV 420 对应的是图片颜色采样格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        //比特率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
        //帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
        //I 帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //创建生成MP4初始化对象
        } catch (IOException e) {
            e.printStackTrace();
        }
        //进入配置状态
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //进行生命周期执行状态
        mediaCodec.start();
    }

    private byte[] getInputBuffer() {
        byte[] input = (byte[]) HookPreviewManager.getInstance().getYUVQueue().poll();
        if (input == null) {
            return null;
        }
        File nv21File =  new File(HookPreviewManager.getInstance().getCacheDirectoryPath() + "/preview_nv21.yuv");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(nv21File);
            fileOutputStream.write(input);
            fileOutputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        File yuv420pFile = new File(HookPreviewManager.getInstance().getCacheDirectoryPath() + "/preview_420p.yuv");
        String nv21To420pCmd = String.format("-pix_fmt nv21 -s %sx%s -i %s -pix_fmt yuv420p -y %s"
                , mHeight, mWidth, nv21File.getAbsolutePath(), yuv420pFile.getAbsolutePath());
        int rc = FFmpeg.execute(nv21To420pCmd);
        if (rc != RETURN_CODE_SUCCESS) {
            Config.printLastCommandOutput(Log.INFO);
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(yuv420pFile);
            fileInputStream.read(input);
            fileInputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return input;
    }

    public void close() {
        isMuxFinish = true;
        try {
            mEncodeThread.join(1000);
        } catch (InterruptedException e) {
            DexposedBridge.log("InterruptedException " + e);
        }
        DexposedBridge.log(" EncodeThread isAlive: " + mEncodeThread.isAlive());
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        try {
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MediaMuxerManager.getInstance().close();
        mediaCodec = null;
        instance = null;
    }

    public void startEncode(Camera.Size size) {
        frameIndex = 0;
        isMuxFinish = false;
        initMediaCodec(size);
        mEncodeThread = new VideoEncoderThread();
        mEncodeThread.setName("VideoEncoderThread");
        mEncodeThread.start();
    }

    class VideoEncoderThread extends Thread {
        @Override
        public void run() {
            super.run();
            long startTime = System.nanoTime();
            while (!isMuxFinish) {
                try {
                    // 拿到有空闲的输入缓存区下标
                    int inputBufferId = mediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferId >= 0) {
                        //有效的空的缓存区
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                        byte[] tempByte = getInputBuffer();
                        if (tempByte == null) {
                            break;
                        }
                        inputBuffer.put(tempByte);
                        frameIndex += 1;
                        long presentationTime = frameIndex * 50 * 1000;
                        //将数据放到编码队列
                        mediaCodec.queueInputBuffer(inputBufferId, 0, tempByte.length, presentationTime, 0);
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    //得到成功编码后输出的out buffer Id
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    if (outputBufferId >= 0 ) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                        byte[] out = new byte[bufferInfo.size];
                        outputBuffer.get(out);
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        // 将编码后的数据写入到MP4复用器
                        MediaMuxerManager.getInstance().writeSampleData(mTrackIndex, outputBuffer, bufferInfo);
                        DexposedBridge.log("write to mp4: " + bufferInfo.size);
                        //释放output buffer
                        mediaCodec.releaseOutputBuffer(outputBufferId, false);
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        DexposedBridge.log("INFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                        mTrackIndex = MediaMuxerManager.getInstance().addTrack(mediaFormat);
                        MediaMuxerManager.getInstance().start();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    continue;
                }
            }
        }
    }
}
