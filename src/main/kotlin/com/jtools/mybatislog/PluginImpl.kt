package com.jtools.mybatislog

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LanguageTextField
import com.lhstack.tools.plugins.IPlugin
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class PluginImpl : IPlugin {

    companion object {
        var cache = hashMapOf<String, JComponent>()
    }

    override fun install() {
        StarterJavaProgramPatcher.registry()
    }

    override fun unInstall() {
        StarterJavaProgramPatcher.unRegistry()
    }

    override fun installRestart(): Boolean {
        return true
    }

    override fun createPanel(project: Project): JComponent {
        return cache.computeIfAbsent(project.locationHash) {
            JPanel(BorderLayout()).apply {
                val state = SupportState.getInstance(project)
                val textField = object:LanguageTextField(PlainTextLanguage.INSTANCE, project, state.supportClasses, false){
                    override fun getEditor(): Editor? {
                        return super.getEditor()?.apply {
                            Disposer.register(project){
                                EditorFactory.getInstance().releaseEditor(this)
                            }
                            val settings = this.settings
                            settings.isLineNumbersShown = true
                            settings.isUseSoftWraps = true
                            settings.isFoldingOutlineShown = true
                        }
                    }
                }
                textField.addDocumentListener(object : DocumentListener {
                    override fun documentChanged(event: DocumentEvent) {
                        state.supportClasses = event.document.text
                    }
                })
                this.add(JPanel(BorderLayout()).apply {
                    this.add(ComboBox(state.supportSqlFormatTypes).apply {
                        this.addItemListener {
                            state.sqlFormatType = it.item.toString()
                        }
                    }, BorderLayout.EAST)
                }, BorderLayout.NORTH)
                this.add(textField, BorderLayout.CENTER)
            }
        }
    }

    override fun support(jToolsVersion: Int): Boolean = jToolsVersion >= 103

    override fun pluginIcon(): Icon? = IconLoader.findIcon("icons/mybatis.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon? = IconLoader.findIcon("icons/mybatis_tab.svg", PluginImpl::class.java)

    override fun pluginName(): String = "jtools-mybatis-log"

    override fun pluginDesc(): String = "jtools-mybatis-log"

    override fun pluginVersion(): String = "v1.0.1"
}