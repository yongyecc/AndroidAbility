package cn.yongye.androidability.common;

import android.content.Context;

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
            context.getResources().getString(resourceId);
        } catch (Exception exception) {
            exception.printStackTrace();
            LogUtil.e(TAG, String.format("[Error][getStringById] context=%s resourceid=%s",
                    context, resourceId));
        }
        return resValue;
    }
}
