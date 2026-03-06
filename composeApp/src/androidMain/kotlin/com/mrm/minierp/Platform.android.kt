package com.mrm.minierp

import android.content.Intent
import android.net.Uri
import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun openUrl(url: String) {
    // Note: This requires a context. In a real app, you'd probably use a CompositionLocal or pass it down.
    // For now, we'll leave it as a placeholder or handle it via a shared state if needed.
    // However, since we are troubleshooting a JVM issue, let's focus on JVM.
}
