package cn.yongye.androidability.opengl.activity;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedHashMap;

import cn.yongye.androidability.common.ViewUtils;
import cn.yongye.androidability.opengl.adapter.OpenGLRecelerViewAdapter;
import cn.yongye.androidability.R;

public class OpenGLDemoListActivity extends Activity {

    RecyclerView recyclerView;
    OpenGLRecelerViewAdapter openGLRecelerViewAdapter;
    LinkedHashMap<Integer, String> listData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl);

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
        listData.put(R.string.draw_a_triangle,
                ViewUtils.getStringById(this, R.string.draw_a_triangle));
    }

    private void initView() {
        recyclerView = findViewById(R.id.openGLRececlerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(OpenGLDemoListActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        openGLRecelerViewAdapter = new OpenGLRecelerViewAdapter(this, listData);
        recyclerView.setAdapter(openGLRecelerViewAdapter);
    }
}
