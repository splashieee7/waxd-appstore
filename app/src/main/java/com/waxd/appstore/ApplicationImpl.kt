package com.waxd.appstore

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.waxd.appstore.autoupdate.AutoUpdatePrefs
import com.waxd.appstore.core.notificationManager
import com.waxd.appstore.util.ActivityUtils

class ApplicationImpl : Application(), ActivityLifecycleCallbacks {
    companion object {
        @SuppressLint("StaticFieldLeak") // app context is a singleton
        // nullable type is used instead of lateinit because initialization checks for lateinit vars
        // in companion object are broken as of Kotlin 1.7
        var baseAppContext: Context? = null

        const val TAG = "ApplicationImpl"
        const val JOB_SCHEDULER_JOB_ID_AUTO_UPDATE = 1000
        const val JOB_SCHEDULER_JOB_ID_UPDATE_CHECK = 1001

        fun exitIfNotInitialized() {
            if (baseAppContext == null) {
                // see https://issuetracker.google.com/issues/160946170
                Log.e(TAG, "custom Application subclass wasn't initialized, " +
                        "likely due to an Android bug, calling System.exit(1)")
                System.exit(1)
            }
        }
    }

    // called before ContentProviders are initialized, onCreate() is called after
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        baseAppContext = base

        PackageStates.requestRepoUpdateNoSuspend()
        PackageStates.init()

        val activeNotifications = notificationManager.activeNotifications
        Notifications.init(activeNotifications)
        ActivityUtils.init(activeNotifications)

        AutoUpdatePrefs.setupJobs()

        // Material You dynamic color intentionally not applied: the waxd palette
        // in values-night/themes.xml is the fixed brand look, not system wallpaper
        // colors. Forcing dark mode always (rather than following the system
        // setting) is what makes that values-night theme the one that's ever
        // shown, matching the rest of the waxd suite always rendering dark.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PackageStates.onResourceConfigChanged()
    }

    override fun onActivityResumed(activity: Activity) {
        ActivityUtils.onActivityResumedOrPaused(activity, true)
    }

    override fun onActivityPaused(activity: Activity) {
        ActivityUtils.onActivityResumedOrPaused(activity, false)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
