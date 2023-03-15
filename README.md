# Android使用的能力

学习Android使用到的能力，编写示例代码总结学习成果。

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

# 【第三方库】ButterKnife-BindView

## 准备

Android Library 项目使用BindView能力。

1. 添加插件到库项目的编译脚本里`mylibrary/build.gradle`
```
buildscript {
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath 'com.jakewharton:butterknife-gradle-plugin:10.2.3'
  }
}
```

2. 应用插件
```
apply plugin: 'com.jakewharton.butterknife'
```

3. 使用BindView时，用R2替代R类
```java
class ExampleActivity extends Activity {
  @BindView(R2.id.user) EditText username;
  @BindView(R2.id.pass) EditText password;
}
```

## 参考

【1】 https://github.com/JakeWharton/butterknife