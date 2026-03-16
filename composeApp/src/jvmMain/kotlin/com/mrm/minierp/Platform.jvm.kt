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
