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

基于MediaRecoder组件的录屏功能.

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

用户手动同意后，启动Service开始进行录屏.
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
            mMediaProjection = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, mResultData);
            mMediaRecorder = createMediaRecorder();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
            mMediaRecorder.start();
            RECORD_STATUS = true;
        }
        return Service.START_NOT_STICKY;
    }
 ```