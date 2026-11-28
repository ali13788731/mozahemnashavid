package com.khalvat.app.protect

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/** Runs every ~15 minutes (also after reboot) and raises an alert if a permission or role was revoked. */
class ProtectionJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Notifier.refreshProtectionAlert(applicationContext)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        private const val JOB_ID = 4242

        fun schedule(context: Context) {
            val app = context.applicationContext
            val scheduler = app.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return
            scheduler.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(app, ProtectionJobService::class.java))
                    .setPeriodic(15 * 60 * 1000L)
                    .setPersisted(true)
                    .build()
            )
        }
    }
}
