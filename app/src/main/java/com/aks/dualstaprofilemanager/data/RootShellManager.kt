package com.aks.dualstaprofilemanager.data

import android.util.Base64
import com.topjohnwu.superuser.Shell

object RootShellManager {
    const val MODULE_DIR = "/data/adb/modules/onyx_dualsta_overlay"
    const val CONFIG_PATH = "$MODULE_DIR/profiles.conf"
    const val BACKUP_PATH = "$MODULE_DIR/profiles.conf.bak"
    const val TMP_PATH = "$MODULE_DIR/profiles.conf.tmp"
    const val MODULE_PROP = "$MODULE_DIR/module.prop"
    const val PID_PATH = "/data/local/tmp/dualsta-helper.pid"
    const val AUTO_LOG = "/data/local/tmp/dualsta-autoconnect.log"
    const val HELPER_LOG = "/data/local/tmp/dualsta-helper.log"

    fun isRootAvailable(): Boolean {
        return Shell.getShell().isRoot
    }

    fun checkModuleExists(): Boolean {
        val result = Shell.cmd("test -d $MODULE_DIR && test -f $MODULE_PROP && test -f $CONFIG_PATH").exec()
        return result.isSuccess
    }

    fun readModuleProp(): String {
        val result = Shell.cmd("cat $MODULE_PROP").exec()
        return if (result.isSuccess) result.out.joinToString("\n") else ""
    }

    fun readConfig(): String {
        val result = Shell.cmd("cat $CONFIG_PATH").exec()
        if (!result.isSuccess) throw RuntimeException("Failed to read profiles.conf via root")
        return result.out.joinToString("\n")
    }

    fun saveConfigAtomic(newConfigContent: String): Boolean {
        try {
            val parsed = ConfigParser.parseConfig(newConfigContent)
            if (parsed.isEmpty()) return false
        } catch (e: Exception) {
            return false
        }

        val base64Data = Base64.encodeToString(newConfigContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val script = """
            set -e
            printf '%s' '$base64Data' | base64 -d > $TMP_PATH
            chown root:root $TMP_PATH
            chmod 600 $TMP_PATH
            if [ -f $CONFIG_PATH ]; then
                cp -f $CONFIG_PATH $BACKUP_PATH
                chmod 600 $BACKUP_PATH
            fi
            mv -f $TMP_PATH $CONFIG_PATH
            sync
        """.trimIndent()

        val result = Shell.cmd(script).exec()
        return result.isSuccess
    }

    fun restoreBackup(): Boolean {
        val script = """
            set -e
            if [ -f $BACKUP_PATH ]; then
                cp -f $BACKUP_PATH $CONFIG_PATH
                chown root:root $CONFIG_PATH
                chmod 600 $CONFIG_PATH
                sync
                exit 0
            else
                exit 1
            fi
        """.trimIndent()
        return Shell.cmd(script).exec().isSuccess
    }

    fun getWlanLink(interfaceName: String): String {
        val result = Shell.cmd("iw dev $interfaceName link").exec()
        return result.out.joinToString("\n")
    }

    fun getHelperPid(): Int? {
        val result = Shell.cmd("cat $PID_PATH").exec()
        if (!result.isSuccess) return null
        return result.out.firstOrNull()?.trim()?.toIntOrNull()
    }

    fun restartSecondaryHelper(): Boolean {
        val pid = getHelperPid()
        val script = if (pid != null && pid > 0) {
            """
                if ps -p $pid -o args= | grep -q 'app_process.*DualStaRequest'; then
                    kill $pid
                    rm -f $PID_PATH
                fi
                sync
            """.trimIndent()
        } else {
            "rm -f $PID_PATH; sync"
        }
        val result = Shell.cmd(script).exec()
        return result.isSuccess
    }

    fun readLog(path: String, maxLines: Int = 100): List<String> {
        val result = Shell.cmd("tail -n $maxLines $path").exec()
        return if (result.isSuccess) result.out else emptyList()
    }

    fun clearLogs(): Boolean {
        val result = Shell.cmd("true > $AUTO_LOG && true > $HELPER_LOG && sync").exec()
        return result.isSuccess
    }
}
