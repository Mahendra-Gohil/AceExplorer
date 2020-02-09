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

package com.siju.acexplorer

import android.app.Application
import android.os.StrictMode
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.kobakei.ratethisapp.RateThisApp
import io.fabric.sdk.android.Fabric

private const val RATE_APP_CRITERIA_INSTALL_DAYS = 7
private const val RATE_APP_CRITERIA_LAUNCH_TIMES = 25

class AceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        appContext = this
        initCrashlytics()
        initRateApp()
        setupPerformanceCheckers()
    }

    private fun initCrashlytics() {
        Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
                .also { crashlyticsKit ->
                    Fabric.with(this, crashlyticsKit)
                }
    }

    private fun initRateApp() {
        val config = RateThisApp.Config(RATE_APP_CRITERIA_INSTALL_DAYS, RATE_APP_CRITERIA_LAUNCH_TIMES)
        RateThisApp.init(config)
    }

    private fun setupPerformanceCheckers() {
        if (BuildConfig.DEBUG) {
            setStrictMode()
        }
    }

    private fun setStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll()
                .penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build())
    }

    companion object {
        lateinit var appContext: AceApplication
            private set
    }

}
