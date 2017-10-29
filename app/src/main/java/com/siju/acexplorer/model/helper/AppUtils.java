/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.model.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.siju.acexplorer.R;


public class AppUtils {

    public static Drawable getAppIcon(Context context, String url) {


        Drawable icon = null;
        try {
            ApplicationInfo appInfo = getAppInfo(context, url);

            if (appInfo != null) {
                icon = appInfo.loadIcon(context.getPackageManager());
            }
//            Log.d("AppUtils", "getAppIcon: icon:"+icon);
            return icon;
        } catch (Exception e) {
//            e.printStackTrace();
            return ContextCompat.getDrawable(context, R.drawable.ic_apk_green);

        }
    }

    public static ApplicationInfo getAppInfo(Context context, String path) {
        ApplicationInfo appInfo;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(path,
                                                                                        PackageManager
                                                                                                .GET_ACTIVITIES);
            if (packageInfo == null) {
                return null;
            }
            appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
        } catch (Exception e) {
//            e.printStackTrace();
            return null;

        }
        return appInfo;
    }

    public static Drawable getAppIconForFolder(Context context, String packageName) {

        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
