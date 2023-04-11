# Android使用的能力

学习Android使用到的能力，编写示例代码总结学习成果。

[TOC]

# Handler

帮助实现线程间通信。

## 更新UI数据

动态更新Activity界面上面的数据。3s后，让界面上的数字每秒后增加1。

1. UI线程创建Handler，负责接收普通线程发送的数字增加消息更新Activity界面。
```java
	Handler uiHandler = null;
	
	public void initHandler(){
        uiHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.d(TAG, String.format("[uiHandler][handleMessage]Message=%s", msg));

                switch (msg.what){
                    case Config.ADD_ONE:
                        number++;
                        textView.setText(String.valueOf(number));
                        break;
                    default:
                        Log.d(TAG, String.format("[uiHandler][handleMessage]Other what of message %s", msg.what));
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }
```

2. 创建普通线程，使用UI线程的Handler向UI线程发送数字增加消息。

```java
public void startAddNumberThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    while (true) {
                        Message message = uiHandler.obtainMessage();
                        message.what = Config.ADD_ONE;
                        uiHandler.sendMessage(message);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
```

3. 实现效果。

![demo1](https://img-blog.csdnimg.cn/5329dacf425a404d918b35ed4aced64f.gif)

# 录屏

## MediaRecorder

基于MediaRecoder组件的录屏功能,点击开始录屏，再次点击结束录屏.

### 录屏过程

1. 点击事件
   a. 检查录屏权限
   b. 开始录屏

```java
//DemoListAdapter.java
//Click事件
        //textView_demo_name
        holder.textView_demo_name.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                 String demo_name = (String) ((TextView) v).getText();
                 LogUtil.d(TAG, String.format("[OnClick] item: %s", demo_name));
                 if (demo_name == null) {
                     return;
                 }
                 //MediaRecorder录屏.
                 if (demo_name.equals(ViewUtils.getStringById(v.getContext(),
                         R.string.screen_record_mediarecoder))) {
                     ScreenRecordManager.SCREEN_RECORD_TYPE = 1;
                     //检查权限并申请屏幕共享权限
                     PermissionUtil.checkAndRequestMorePermissions(MainActivity.mainActivity, screenPermission,
                             REQUEST_SCREEN_RECORDER_PERMISSION);
                     //开启屏幕录制(MediaRecorder)
                     if (!RECORD_STATUS) {
                         ScreenRecordManager.getInstance().startScreenRecord(MainActivity.mainActivity);
                     } else {
                         ScreenRecordManager.getInstance().stopScreenRecord(MainActivity.mainActivity);
                     }
                 }
                 }
```

2. 开始录屏

向系统申请开始录屏.

```java
//ScreenRecordManager.java
/**
 * 开始录屏.
 */
    public void startScreenRecord(Activity activity) {
        if (mMediaProjectionManager == null) {
        mMediaProjectionManager = getMediaProjectionManager(activity);
        }
        Intent screenCaptureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        activity.startActivityForResult(screenCaptureIntent, REQUEST_SCREEN_RECORDER_CODE);
        }
```

3. 用户手动同意后，启动Service开始进行录屏.
```java
//MainActivity.java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SCREEN_RECORDER_CODE:
                    Intent screenRecordIntent = new Intent(this, ScreenRecordService.class);
                    screenRecordIntent.putExtra("intent", data);
                    screenRecordIntent.putExtra("resultCode", resultCode);
                    startService(screenRecordIntent);
                    break;
                default:
                    break;
            }
        }
    }
```

MediaRecoder录屏
```java
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
 ```

## mediacodec+MediaMuxer录屏

点击开始录屏，再次点击结束录屏.

### 录屏过程

1. 点击事件（同上）
2. 开始录屏（同上）
3. 开启录屏线程MediaMuxerScreenRecordThread

```java
//MediaMuxerScreenRecordThread.java
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
```

### 屏幕静止检测

##### 方案1: 计算间隔数秒间的两帧画面相似度值

```java
//MediaMuxerScreenRecordThread.java

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
```


##### 方案2: (Pixel设备) mediacodec解码出的关键帧之间的时间间隔。