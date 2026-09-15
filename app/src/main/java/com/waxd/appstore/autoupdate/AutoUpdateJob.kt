package com.waxd.appstore.autoupdate

import android.app.job.JobParameters
import android.app.job.JobService
import android.net.Network
import android.util.Log
import com.waxd.appstore.ApplicationImpl
import com.waxd.appstore.Notifications
import com.waxd.appstore.PackageStates
import com.waxd.appstore.core.InstallParams
import com.waxd.appstore.core.collectOutdatedPackageGroups
import com.waxd.appstore.core.startPackageUpdate
import com.waxd.appstore.util.checkMainThread
import com.waxd.appstore.util.isAppInstallationAllowed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private const val TAG = "AutoUpdateJob"

class AutoUpdateJob : JobService() {
    private var activeJobs: List<Job>? = null

    override fun onStartJob(jobParams: JobParameters): Boolean {
        ApplicationImpl.exitIfNotInitialized()
        Log.d(TAG, "onStartJob")
        checkMainThread()

        if (!isAppInstallationAllowed()) {
            return false
        }

        val network: Network? = jobParams.network

        if (network == null) {
            Log.d(TAG, "jobParams.network == null")
            return false
        }

        val installParams = InstallParams(network, isUpdate = true, isUserInitiated = false)

        CoroutineScope(Dispatchers.Main).launch {
            val repoUpdateError = PackageStates.requestRepoUpdateRetrying()

            if (repoUpdateError != null) {
                showUpdateCheckFailedNotification(repoUpdateError)
            } else {
                val outdatedPackageGroups = collectOutdatedPackageGroups()

                if (outdatedPackageGroups.isEmpty()) {
                    showAllUpToDateNotification()
                } else {
                    Notifications.cancel(Notifications.ID_AUTO_UPDATE_JOB_STATUS)

                    val jobs = startPackageUpdate(installParams, outdatedPackageGroups)
                    check(jobs.isNotEmpty())

                    activeJobs = jobs
                    // Wait for packages to be committed, but don't wait for installation to complete.
                    // OS will continue installing the committed packages even if app process dies,
                    // and will spawn it automatically when session completes (by sending a broadcast
                    // to PkgInstallerStatusReceiver)
                    jobs.joinAll()

                    activeJobs = null
                }
            }

            jobFinished(jobParams, false)
            Log.d(TAG, "finished")
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        checkMainThread()

        // There are many reasons the job might be stopped, important ones are:
        // - device is no longer idle
        // - network type has changed from what is specified in job parameters

        Log.d(TAG, "onStopJob, reason ${params.stopReason}")

        val jobs = activeJobs
        activeJobs = null
        if (jobs != null) {
            jobs.forEach { it.cancel() }
            // "true" means "reschedule the job again as soon as possible"
            return true
        } else {
            return false
        }
    }
}
