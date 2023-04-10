package cn.yongye.androidability.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;


import cn.yongye.androidability.R;

public class DemoListAdapter extends RecyclerView.Adapter<DemoListAdapter.ViewHolder> {

    public HashMap<Integer, String>  mData;
    LayoutInflater mInflater;
    ViewHolder mViewHolder;

    public DemoListAdapter(Context context, HashMap<Integer, String> data) {
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
        holder.textView_demo_name.setText(mData.get(0));
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
