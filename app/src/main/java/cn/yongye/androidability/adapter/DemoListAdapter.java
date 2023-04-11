package cn.yongye.androidability.adapter;

import static cn.yongye.androidability.screenrecord.service.ScreenRecordService.REQUEST_SCREEN_RECORDER_PERMISSION;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import cn.yongye.androidability.R;
import cn.yongye.androidability.activity.MainActivity;
import cn.yongye.androidability.common.LogUtil;
import cn.yongye.androidability.common.PermissionUtil;
import cn.yongye.androidability.common.ViewUtils;
import cn.yongye.androidability.screenrecord.ScreenRecordBean;
import cn.yongye.androidability.screenrecord.ScreenRecordManager;

public class DemoListAdapter extends RecyclerView.Adapter<DemoListAdapter.ViewHolder> {

    private static final String TAG = DemoListAdapter.class.getSimpleName();
    public LinkedHashMap<Integer, String> mData;
    LayoutInflater mInflater;


    public DemoListAdapter(Context context, LinkedHashMap<Integer, String> data) {
        this.mData = data;
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_demo_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 列表子项中的view组件初始化.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //初始化列表子项的textView_demo_name
        String demo_name = null;
        Iterator iterator = mData.entrySet().iterator();
        for (int i = 0; i < mData.size(); i++) {
            Map.Entry item = (Map.Entry) iterator.next();
            if (i != position) {
                continue;
            }
            demo_name = (String) item.getValue();
            break;
        }
        holder.textView_demo_name.setText(demo_name);

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
                     ScreenRecordBean.SCREEN_RECORD_TYPE = ScreenRecordBean.RECORD_TYPE_MEDIARECORD;
                     //检查权限并申请屏幕共享权限
                     PermissionUtil.checkAndRequestMorePermissions(MainActivity.mainActivity,
                             ScreenRecordBean.screenPermission, REQUEST_SCREEN_RECORDER_PERMISSION);
                     //开启屏幕录制(MediaRecorder)
                     if (!ScreenRecordBean.RECORD_STATUS) {
                         ScreenRecordManager.getInstance().startScreenRecord(MainActivity.mainActivity);
                     } else {
                         ScreenRecordManager.getInstance().stopScreenRecord(MainActivity.mainActivity);
                     }
                 } else if (demo_name.equals(ViewUtils.getStringById(v.getContext(),
                         R.string.screen_record_mediamuxer))) {
                     ScreenRecordBean.SCREEN_RECORD_TYPE = ScreenRecordBean.RECORD_TYPE_MEDIAMUXER;
                     //开启屏幕录制(MediaRecorder)
                     if (!ScreenRecordBean.RECORD_STATUS) {
                         ScreenRecordManager.getInstance().startScreenRecord(MainActivity.mainActivity);
                     } else {
                         ScreenRecordManager.getInstance().stopScreenRecord(MainActivity.mainActivity);
                     }

                 }

            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    /**
     * 列表子项中的所有UI组件.
     */
    class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView_demo_name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView_demo_name = itemView.findViewById(R.id.text_demo_item);
        }
    }
}
