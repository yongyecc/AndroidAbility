
package cn.yongye.androidability.screenrecord;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.yongye.androidability.activity.MainActivity;
import cn.yongye.androidability.common.LogUtil;
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
                prepareEncoder();
                mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
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
            if(image!=null){
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                stream.close();
                if(oldBitmap == null) {
                    oldBitmap = bmp;
                    LogUtil.i(TAG, "Init 5 second before bitmap");
                    return;
                }
                //5s一次差异化比较
                if (System.currentTimeMillis() - lastTime >= 5000) {
                    lastTime = System.currentTimeMillis();
                    int diffNumber = SimilarPicture.diff(bmp, oldBitmap);
                    oldBitmap = bmp;
                    LogUtil.d(TAG, "Diff=" + diffNumber);
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
}