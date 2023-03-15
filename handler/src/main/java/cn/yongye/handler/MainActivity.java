package cn.yongye.handler;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thundersec.mylibrary.LibMainActivity;

import java.sql.Array;
import java.util.ArrayList;

import cn.yongye.handler.common.Config;
import cn.yongye.handler.demo.bindview.BindViewActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.listView);

        ArrayList<String> demos = new ArrayList<>();
        demos.add("动态更新Activity界面");
        demos.add("两个线程互相发送消息");
        demos.add("BindView测试");
        ItemAdapter itemAdapter = new ItemAdapter(this, demos);
        listView.setAdapter(itemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this, String.format("Click %s item.", i),
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                String targetClass = null;
                switch (i){
                    case 0:
                        targetClass = "cn.yongye.handler.demo.UpdateUIThread";
                        break;
                    case 1:
                        targetClass = "cn.yongye.handler.demo.Demo2ThreadAToThreadB";
                        break;
                    case 2:
                        targetClass = LibMainActivity.class.getName();
                        break;
                }
                intent.setComponent(new ComponentName(getPackageName(), targetClass));
                startActivity(intent);
            }
        });
    }

    public class ItemAdapter extends BaseAdapter{

        ArrayList<String> mData  = null;
        Context mContext = null;

        public ItemAdapter(Context context, ArrayList<String> mData) {
            this.mContext = context;
            this.mData = mData;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item, viewGroup, false);
            TextView nameView = view.findViewById(R.id.DemoName);
            nameView.setText(mData.get(i));
            return view;
        }
    }
}