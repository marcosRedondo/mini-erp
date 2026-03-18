package com.mrm.minierp

import android.content.Intent
import android.net.Uri
import android.os.Build

object AndroidContextProvider {
    var context: android.content.Context? = null
    var onPickDirectory: ((onDirectoryPicked: (String) -> Unit) -> Unit)? = null
}

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidContextProvider.context?.startActivity(intent)
}

actual fun pickDirectory(onDirectoryPicked: (String) -> Unit) {
    AndroidContextProvider.onPickDirectory?.invoke(onDirectoryPicked)
}
