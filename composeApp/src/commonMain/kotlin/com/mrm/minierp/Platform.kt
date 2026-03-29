package com.mrm.minierp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun openUrl(url: String)

expect fun pickDirectory(onDirectoryPicked: (String) -> Unit)

expect fun pickImageAsBase64(onImagePicked: (String?) -> Unit)