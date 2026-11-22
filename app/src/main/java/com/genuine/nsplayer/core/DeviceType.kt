package com.genuine.nsplayer.core

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

enum class DeviceMode {
    MOBILE,
    TELEVISION
}

object DeviceDetector {
    fun getDeviceMode(context: Context): DeviceMode {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return DeviceMode.TELEVISION
        }

        val packageManager = context.packageManager
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

        return if (hasLeanback || hasNoTouch) {
            DeviceMode.TELEVISION
        } else {
            DeviceMode.MOBILE
        }
    }
}
