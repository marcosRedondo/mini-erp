package com.mrm.minierp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private var directoryCallback: ((String) -> Unit)? = null
    private var imageCallback: ((String?) -> Unit)? = null

    private val pickDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            directoryCallback?.invoke(it.toString())
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    imageCallback?.invoke(base64)
                } ?: imageCallback?.invoke(null)
            } catch (e: Exception) {
                imageCallback?.invoke(null)
            }
        } ?: imageCallback?.invoke(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidContextProvider.context = this
        AndroidContextProvider.onPickDirectory = { callback ->
            directoryCallback = callback
            pickDirectoryLauncher.launch(null)
        }
        AndroidContextProvider.onPickImage = { callback ->
            imageCallback = callback
            pickImageLauncher.launch("image/*")
        }


        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}