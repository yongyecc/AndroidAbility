
package cn.yongye.androidability.screenrecord;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.yongye.androidability.activity.MainActivity;
import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.common.PermissionUtil;
import cn.yongye.androidability.common.SimilarPicture;


/**
 * 基于mediacodec+mediamuxer的录屏线程
 */
public class MediaMuxerScreenRecordThread extends Thread{
    private static final String TAG = MediaMuxerScreenRecordThread.class.getSimpleName();
    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private int mDpi;
    private String mDstPath;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC; // H.264 Advanced
    private static final int TIMEOUT_US = 10000;
    private MediaCodec mEncoder;
    private Surface mSurface;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    private int mVideoTrackIndex = -1;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;
    ImageReader imageReader;
    public static final int NAL_SLICE = 1;      //非关键帧
    public static final int NAL_SLICE_IDR = 5;  //I关键帧
    public static final int NAL_SPS = 7;        //SPS关键帧
    String tmpFileDirPath = MainActivity.mainActivity.getFilesDir().getAbsolutePath() + File.separator;
    String H264FramFilePath = tmpFileDirPath +  "frame.h264";
    String YUV420PFrameFilePath = tmpFileDirPath + "frame.yuv";
    String NV21FrameFilePath = tmpFileDirPath + "frame_nv21.yuv";

    public MediaMuxerScreenRecordThread(int width, int height, int bitrate, int dpi, MediaProjection mp, String dstPath) {
        super(TAG);
        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        mDpi = dpi;
        mMediaProjection = mp;
        mDstPath = dstPath;
    }

    /**
     * 准备编码器.
     * @throws IOException
     */
    private void prepareEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, ScreenRecordBean.FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, ScreenRecordBean.IFRAME_INTERVAL);
        LogUtil.d(TAG, "created video format: " + format);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
        LogUtil.d(TAG, "created input surface: " + mSurface);
        mEncoder.start();
    }

    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @Override
    public void run() {
        try {
            try {
                mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                //音频
                initAudioRecord();
                initAudioMedicode();
                mAudioMediaCodec.start();
                //视频
                prepareEncoder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display", mWidth, mHeight, mDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface, null, null);
            LogUtil.i(TAG, "created virtual display: " + mVirtualDisplay);
            recordVirtualDisplay();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            release();
        }
    }

    /**
     * 录屏.
     */
    private void recordVirtualDisplay() {
        while (!mQuit.get()) {
            //视频输出
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            //   LogUtil.i(TAG, "dequeue output buffer index=" + index);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 后续输出格式变化
                resetOutputFormat();
            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 请求超时
                try {
                    // wait 10ms
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            } else if (index == 0) {
                // 有效输出
                if (!mMuxerStarted) {
                    throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                }
                encodeToVideoTrack(index);
                mEncoder.releaseOutputBuffer(index, false);
            }
            if (mAudioMediaCodec != null) {
                //音频输出
                //得到 输入缓冲区的索引
                int audioInputID = mAudioMediaCodec.dequeueInputBuffer(0);
                //也是大于等于0 代表 可以输入数据啦
                if (audioInputID >= 0) {
                    ByteBuffer audioInputBuffer = mAudioMediaCodec.getInputBuffer(audioInputID);
                    audioInputBuffer.clear();
                    //从 audiorecord 里面 读取原始的音频数据
                    int read = mAudiorecord.read(byteBuffer, 0, audioBufferSize);
                    if (read < audioBufferSize) {
                        System.out.println(" 读取的数据" + read);
                    }
                    //上面read可能小于audioBufferSize  要注意
                    audioInputBuffer.put(byteBuffer, 0, read);
                    //入列  注意下面的时间，这个是确定这段数据 时间的 ，视频音频 都是一段段的数据，每个数据都有时间 ，这样播放器才知道 先播放那个数据
                    // 串联起来 就是连续的了
                    mAudioMediaCodec.queueInputBuffer(audioInputID, 0, read, System.nanoTime() / 1000L, 0);
                }
                //音频输出
                int audioOutputID = mAudioMediaCodec.dequeueOutputBuffer(audioInfo, 0);
                LogUtil.d(TAG, "audio flags " + audioInfo.flags);
                if (audioOutputID >= 0) {
                    audioInfo.presentationTimeUs += 1000 * 1000 / ScreenRecordBean.FRAME_RATE;//保持 视频和音频的统一，防止 时间画面声音 不同步
                    if (audioInfo.flags != 2 && mMuxerStarted) {
                        //这里就可以取出数据 进行网络传输
                        ByteBuffer audioOutBuffer = mAudioMediaCodec.getOutputBuffer(audioOutputID);
                        audioOutBuffer.limit(audioInfo.offset + audioInfo.size);//这是另一种 和上面的 flip 没区别
                        audioOutBuffer.position(audioInfo.offset);
                        mMuxer.writeSampleData(audioIndex, audioOutBuffer, audioInfo);//写入
                        LogUtil.d(TAG, String.format("Write audio(index=%s) bytes len=%s", audioIndex, audioInfo.size));
                    }
                    //释放缓冲区
                    mAudioMediaCodec.releaseOutputBuffer(audioOutputID, false);
                } else if (audioOutputID == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioFormat = mAudioMediaCodec.getOutputFormat();
                    audioIndex = mMuxer.addTrack(audioFormat);
                    //注意 这里  只在start  视频哪里没有这个，这个方法只能调用一次
                    //mMuxer.start();
                }
            }
        }
    }
    /**
     * 硬解码获取实时帧数据并写入mp4文件
     *
     * @param index
     */
    private void encodeToVideoTrack(int index) {
        // 获取到的实时帧视频数据
        ByteBuffer encodedData = mEncoder.getOutputBuffer(index);
        //关键帧解码时进行图片相似度比较
        onEncodedAvcFrame(encodedData, mBufferInfo);

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer
            // when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            LogUtil.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            LogUtil.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        }
        if (encodedData != null) {
            mBufferInfo.presentationTimeUs += 1000 * 1000 / ScreenRecordBean.FRAME_RATE;
            mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
        }
    }

    private byte[] sps_pps_buf;
    static Bitmap oldBitmap;
    static long lastTime = 0;
    private void onEncodedAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
        if (lastTime == 0) {
            lastTime = System.currentTimeMillis();
        }
        int offset = 4;
        //判断帧的类型 https://zhuanlan.zhihu.com/p/25655203
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = bb.get(offset) & 0x1f;
        if (type == NAL_SPS) {
            try {
                //打印发现这里将 SPS帧和 PPS帧合在了一起发送
                // SPS为 [4，len-8]
                // PPS为后4个字节
                sps_pps_buf = new byte[vBufferInfo.size];
                bb.get(sps_pps_buf);
                LogUtil.d(TAG, "SPS帧: " + sps_pps_buf.length);
                onImageData(sps_pps_buf, H264FramFilePath);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else if (type == NAL_SLICE_IDR) {
            // I帧
            LogUtil.d(TAG, "I帧");
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            //在原始数据前加上sps和pps的数据，才可组成一个完整的关键帧
            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
            onImageData(newBuf, H264FramFilePath);
        }  else if (type == NAL_SLICE) {
            // 非关键帧
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            LogUtil.d(TAG, "非关键帧: " + bytes.length);
        } else {
            LogUtil.d(TAG, "帧: " + type);
        }
    }

    public void onImageData(byte[] buf, String h264Path) {
        try {
            //5s一次差异化比较
            if (System.currentTimeMillis() - lastTime >= 5000) {
                FileOutputStream os = new FileOutputStream(h264Path);
                LogUtil.i(TAG, "onImageData  " + buf.length + "  ------  " + os);
                if (null != os) {
                    try {
                        //在字节数组前添加整个帧数据长度
                        byte[] bytes = new byte[buf.length + 3];
                        byte[] head = intToBuffer(buf.length);
                        System.arraycopy(head, 0, bytes, 0, head.length);
                        System.arraycopy(buf, 0, bytes, head.length, buf.length);
                        os.write(bytes);
                        os.flush();
                        bytes = null;
                        head = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //截图相似度比较
                //H264->yuv420p
                //bugfix 使用mobile-ffmpeg库执行有关文件转化等会产生新文件任务是需要删除已存在文件，否则命令会被迫终止。
                new File(YUV420PFrameFilePath).delete();
                int rc = -1;
                rc = FFmpeg.execute(String.format("-i %s -c:v rawvideo -pix_fmt yuv420p %s",
                        H264FramFilePath, YUV420PFrameFilePath));
                if (rc == RETURN_CODE_SUCCESS) {
                    LogUtil.d(TAG, "H264->YUV420P SUCCESSFUL.");
                } else if (rc == RETURN_CODE_CANCEL) {
                    LogUtil.i(TAG, "Command execution cancelled by user.");
                } else {
                    LogUtil.i(TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                    Config.printLastCommandOutput(Log.INFO);
                    return;
                }
                //yuv420p->NV21
                new File(NV21FrameFilePath).delete();
                String cmdYUV2NV21 = String.format("-s %sx%s -pix_fmt yuv420p -i %s -pix_fmt nv21 %s",
                        mWidth, mHeight, YUV420PFrameFilePath, NV21FrameFilePath);
                rc = FFmpeg.execute(cmdYUV2NV21);
                if (rc == RETURN_CODE_SUCCESS) {
                    LogUtil.d(TAG, "YUV420P->YUV21 SUCCESSFUL.");
                } else if (rc == RETURN_CODE_CANCEL) {
                    LogUtil.i(TAG, "Command execution cancelled by user.");
                } else {
                    LogUtil.i(TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                    Config.printLastCommandOutput(Log.INFO);
                }
                //NV21->Bitmap
                File f = new File(NV21FrameFilePath);
                if (f.length() == 0) {
                    return;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
                BufferedInputStream in = null;

                in = new BufferedInputStream(new FileInputStream(f));
                int buf_size = 1024;
                byte[] buffer = new byte[buf_size];
                int len = 0;
                while (-1 != (len = in.read(buffer, 0, buf_size))) {
                    bos.write(buffer, 0, len);
                }
                byte[] nv21Bytes = bos.toByteArray();
                YuvImage image = new YuvImage(nv21Bytes, ImageFormat.NV21, mWidth, mHeight, null);
                if (image != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 80, stream);
                    Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                    stream.close();
                    if (oldBitmap == null) {
                        oldBitmap = bmp;
                        LogUtil.i(TAG, "Init 5 second before bitmap");
                        return;
                    }

                    lastTime = System.currentTimeMillis();
                    int diffNumber = SimilarPicture.diff(bmp, oldBitmap);
                    oldBitmap = bmp;
                    LogUtil.d(TAG, "diff=" + diffNumber);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    //int值转3字节长度的字节数组
    public static byte[] intToBuffer(int value) {
        byte[] src = new byte[3];
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen
        // once
        if (mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        MediaFormat newFormat = mEncoder.getOutputFormat();
        mVideoTrackIndex = mMuxer.addTrack(newFormat);
        mMuxer.start();
        mMuxerStarted = true;
        LogUtil.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    /**
     * 释放录屏用到的组件.
     */
    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        //音频
        if (mAudioMediaCodec != null) {
            mAudioMediaCodec.stop();
            mAudioMediaCodec.release();
            mAudioMediaCodec = null;
        }
        if (mAudiorecord != null) {
            mAudiorecord.stop();
            mAudiorecord.release();
            mAudiorecord = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }


    private AudioRecord mAudiorecord;//录音类
    // 音频源：音频输入-麦克风  我使用其他格式 就会报错
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 44100;
    // 音频通道 默认的 可以是单声道 立体声道
    private final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_DEFAULT;
    // 音频格式：PCM编码   返回音频数据的格式
    private final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    //记录期间写入音频数据的缓冲区的总大小(以字节为单位)。
    private int audioBufferSize = 0;
    //缓冲数组 ，用来读取audioRecord的音频数据
    private byte[] byteBuffer;
    private int audioIndex;//通过MediaMuxer 向本地文件写入数据时候，这个标志是用来确定信道的
    private MediaCodec mAudioMediaCodec;//音频编码器
    private MediaFormat audioFormat;//音频编码器 输出数据的格式
    //这个是每次在编码器 取数据的时候，这个info 携带取出数据的信息，例如 时间，大小 类型之类的  关键帧 可以通过这里的flags辨别
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

    public void setRun(boolean run) {
        isRun = run;
    }

    public volatile boolean isRun = true;//用于控制 是否录制，这个无关紧要

    //录音相关
    //实例化 AUDIO 的编码器
    void initAudioMedicode() throws IOException {
        audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
        //描述要使用的AAC配置文件的键(仅适用于AAC音频格式)。
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioBufferSize << 1);//最大输入

        //这里注意  如果 你不确定 你要生成的编码器类型，就通过下面的 通过类型生成编码器
        mAudioMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        //配置
        mAudioMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    //录音的类，用于给音频编码器 提供原始数据
    @RequiresApi(api = Build.VERSION_CODES.M)
    void initAudioRecord() {
        //得到 音频录制时候 最小的缓冲区大小
        audioBufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        byteBuffer = new byte[audioBufferSize];

        if (!PermissionUtil.checkPermission(MainActivity.mainActivity, Manifest.permission.RECORD_AUDIO)) {
            return;
        }
        mAudiorecord = new AudioRecord.Builder()
                .setAudioSource(AUDIO_SOURCE)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_ENCODING)
                        .setSampleRate(AUDIO_SAMPLE_RATE)
                        .setChannelMask(AUDIO_CHANNEL)
                        .build())
                .setBufferSizeInBytes(audioBufferSize)
                .build();
        //开始录制，这里可以检查一下状态，但只要代码无误，检查是无需的 state
        mAudiorecord.startRecording();
    }
}