package com.tsclown.permission;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class PermissionUtil {

    public static void jumpToAppSetting(Activity activity) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), (String)null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

}
