package com.aks.dualstaprofilemanager

import android.app.Application
import com.topjohnwu.superuser.Shell

class DualStaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER))
    }
}
