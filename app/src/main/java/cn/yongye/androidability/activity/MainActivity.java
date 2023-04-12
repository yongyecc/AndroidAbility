package cn.yongye.androidability.activity;

import static cn.yongye.androidability.screenrecord.service.ScreenRecordService.REQUEST_SCREEN_RECORDER_CODE;
import static cn.yongye.androidability.screenrecord.service.ScreenRecordService.REQUEST_SCREEN_RECORDER_PERMISSION;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.LinkedHashMap;

import cn.yongye.androidability.R;
import cn.yongye.androidability.adapter.DemoListAdapter;
import cn.yongye.androidability.common.ViewUtils;
import cn.yongye.androidability.screenrecord.service.ScreenRecordService;

public class MainActivity extends AppCompatActivity {

    RecyclerView demoListRecyclerView;
    DemoListAdapter demoListAdapter;
    LinkedHashMap<Integer, String> listData;
    public static Activity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;

        initListData();
        initView();
    }

    /**
     * 初始化列表中的数据.
     */
    private void initListData() {
        if (listData == null) {
            listData = new LinkedHashMap<>();
        }
        listData.put(R.string.screen_record_mediarecoder,
                ViewUtils.getStringById(this, R.string.screen_record_mediarecoder));
        listData.put(R.string.screen_record_mediamuxer,
                ViewUtils.getStringById(this, R.string.screen_record_mediamuxer));
        listData.put(R.string.doodle_by_path,
                ViewUtils.getStringById(this, R.string.doodle_by_path));
    }

    /**
     * 初始化UI.
     */
    private void initView() {
        demoListRecyclerView = findViewById(R.id.demoListView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        demoListRecyclerView.setLayoutManager(linearLayoutManager);
        demoListAdapter = new DemoListAdapter(this, listData);
        demoListRecyclerView.setAdapter(demoListAdapter);
    }

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

}