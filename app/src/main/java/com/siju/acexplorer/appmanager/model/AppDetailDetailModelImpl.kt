package com.siju.acexplorer.appmanager.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siju.acexplorer.appmanager.helper.AppHelper
import com.siju.acexplorer.extensions.getInstallerPackage
import com.siju.acexplorer.main.model.helper.FileUtils
import com.siju.acexplorer.main.model.helper.SdkHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppDetailDetailModelImpl @Inject constructor(@ApplicationContext val context: Context) : AppDetailModel {

    private val _versionInfo = MutableLiveData<AppVersionInfo>()

    val versionInfo: LiveData<AppVersionInfo>
        get() = _versionInfo

    private val _appInfo = MutableLiveData<AppInfo>()

    val appInfo: LiveData<AppInfo>
        get() = _appInfo

    private val _permissionInfo = MutableLiveData<PermissionInfo>()

    val permissionInfo: LiveData<PermissionInfo>
        get() = _permissionInfo


    override fun fetchPackageInfo(packageName: String) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName,
                                                                    PackageManager.GET_PERMISSIONS)
            _versionInfo.postValue(createVersionInfo(packageInfo))
            _appInfo.postValue(createAppInfo(packageInfo))
            _permissionInfo.postValue(createPermissionInfo(packageInfo))

        }
        catch (e: PackageManager.NameNotFoundException) {

        }
    }

    @Suppress("DEPRECATION")
    private fun createVersionInfo(packageInfo: PackageInfo): AppVersionInfo {
        val versionCode = if (SdkHelper.isAtleastPie) {
            packageInfo.longVersionCode.toInt()
        }
        else {
            packageInfo.versionCode
        }
        val versionName = packageInfo.versionName

        return AppVersionInfo(versionName, versionCode)
    }

    private fun createAppInfo(packageInfo: PackageInfo): AppInfo {
        val packageName = packageInfo.packageName
        val applicationInfo = packageInfo.applicationInfo
        val appName = applicationInfo.loadLabel(context.packageManager)
        return AppInfo(packageName, appName.toString(), getInstallerSource(packageName),
                       applicationInfo.enabled,
                       getMinSdkVersion(applicationInfo), applicationInfo.targetSdkVersion,
                       FileUtils.convertDate(packageInfo.firstInstallTime),
                       FileUtils.convertDate(packageInfo.lastUpdateTime))
    }

    private fun createPermissionInfo(packageInfo: PackageInfo): PermissionInfo {
        return PermissionInfo(packageInfo.requestedPermissions)
    }

    private fun getMinSdkVersion(applicationInfo: ApplicationInfo): Int {
        return if (SdkHelper.isAtleastNougat) {
            applicationInfo.minSdkVersion
        }
        else {
            0
        }
    }

    private fun getInstallerSource(packageName: String): String {
        return AppHelper.getInstallerSourceName(context, context.packageManager.getInstallerPackage(packageName))
    }


}
