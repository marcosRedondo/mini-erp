package com.mrm.minierp

import java.awt.Desktop
import java.net.URI

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun openUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}

actual fun pickDirectory(onDirectoryPicked: (String) -> Unit) {
    val chooser = javax.swing.JFileChooser().apply {
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Seleccionar carpeta de base de datos"
        approveButtonText = "Seleccionar"
    }
    val result = chooser.showOpenDialog(null)
    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
        onDirectoryPicked(chooser.selectedFile.absolutePath)
    }
}

actual fun pickImageAsBase64(onImagePicked: (String?) -> Unit) {
    val chooser = javax.swing.JFileChooser().apply {
        fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
        dialogTitle = "Seleccionar Logo de la Empresa"
        fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif")
    }
    val result = chooser.showOpenDialog(null)
    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        try {
            val bytes = java.nio.file.Files.readAllBytes(file.toPath())
            val base64 = java.util.Base64.getEncoder().encodeToString(bytes)
            onImagePicked(base64)
        } catch (e: Exception) {
            onImagePicked(null)
        }
    } else {
        onImagePicked(null)
    }
}

