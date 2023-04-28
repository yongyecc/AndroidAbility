package cn.yongye.androidability.common;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    public final static int CODE_REQUEST_CAMERA_PERMISSION = 1;

    /**
     * 检测并请求多个权限 .
     *
     * @param context     context
     * @param permissions 权限列表
     * @param requestCode 请求码
     */
    public static void checkAndRequestMorePermissions(Context context, String[] permissions, int requestCode) {
        List<String> permissionList = checkMorePermissions(context, permissions);
        if (permissionList != null && permissionList.size() > 0) {
            requestMorePermissions(context, permissionList, requestCode);
        }
    }

    /**
     * 检测多个权限 .
     *
     * @param context        context
     * @param permissionList
     * @return 权限集合
     */
    public static List<String> checkMorePermissions(Context context, String[] permissionList) {
        List<String> permissions = new ArrayList<>();
        for (int i = 0; i < permissionList.length; i++) {
            if (!checkPermission(context, permissionList[i]))
                permissions.add(permissionList[i]);
        }
        return permissions;
    }

    /**
     * 检测权限 .
     *
     * @param context    context
     * @param permission 权限
     * @return true：已授权； false：未授权
     */
    public static boolean checkPermission(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求多个权限 .
     *
     * @param context        context
     * @param permissionList 权限列表
     * @param requestCode    请求码
     */
    public static void requestMorePermissions(Context context, List permissionList, int requestCode) {
        String[] permissions = (String[]) permissionList.toArray(new String[permissionList.size()]);
        requestMorePermissions(context, permissions, requestCode);
    }

    /**
     * 请求多个权限 .
     *
     * @param context     context
     * @param permissions 权限列表
     * @param requestCode 请求码
     */
    public static void requestMorePermissions(Context context, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
    }
}
