package cn.yongye.androidability.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.os.Bundle;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import cn.yongye.androidability.R;
import cn.yongye.androidability.adapter.DemoListAdapter;
import cn.yongye.androidability.common.ViewUtils;

public class MainActivity extends AppCompatActivity {

    RecyclerView demoListRecyclerView;
    DemoListAdapter demoListAdapter;
    HashMap<Integer, String> listData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initListData();
        initView();
    }

    /**
     * 初始化列表中的数据.
     */
    private void initListData() {
        if (listData == null) {
            listData = new HashMap<>();
        }
        listData.put(0, "testItem");
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


}