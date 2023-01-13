package com.github.unldenis.yamlenumerable

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import java.io.File
import javax.swing.Icon


class YmlConverterAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)

        val content = file?.contentsToByteArray()?.let { String(it) }

        content?.let {

            val path = file.toNioPath().parent.toFile().toString() + "\\Message.java"

            val dialog = Messages.showYesNoDialog("Parsing to $path", "Status Yml-Converter", null)
            if (dialog == 0) {
                val output = File(path).bufferedWriter()
                output.write(parse(it))
                output.close()

                Messages.showInfoMessage("Done", "Status Yml-Converter")
            }
        }
    }
}