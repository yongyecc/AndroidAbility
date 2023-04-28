package cn.yongye.androidability.common;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class ViewUtils {

    final static String TAG = ViewUtils.class.getSimpleName();


    /**
     * 根据资源ID获取strings.xml里的字符串.
     *
     * @param context .
     * @param resourceId 资源ID.
     * @return  资源值.
     */
    public static String getStringById(Context context, int resourceId) {
        String resValue = null;
        try {
            resValue = context.getResources().getString(resourceId);
        } catch (Exception exception) {
            exception.printStackTrace();
            LogUtil.e(TAG, String.format("[Error][getStringById] context=%s resourceid=%s",
                    context, resourceId));
        }
        return resValue;
    }

    /**
     * 新增Toast展示.
     * @param context   .
     * @param text  .
     */
    public static void showToast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
