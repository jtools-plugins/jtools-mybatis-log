package com.jtools.mybatislog

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import javax.swing.JComponent

class TempProps() {
    var enabled = true
    var ansiCode = "92"
    var colorName: String = "亮绿色"
}

class JToolsMybatisLogConfigurable(val project: Project) : Configurable {

    val pluginState: PluginState = PluginState.getInstance(project)

    val tempProps = TempProps()
    var settingPanel: SettingPanel
    init {
        tempProps.enabled = pluginState.getEnabled()
        tempProps.ansiCode = pluginState.getAnsiCode()
        tempProps.colorName = pluginState.getColorName()
        settingPanel = SettingPanel(project, tempProps)
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "JToolsMybatisLog"

    override fun createComponent(): JComponent = settingPanel

    override fun isModified(): Boolean = tempProps.enabled != pluginState.getEnabled() ||
            tempProps.ansiCode != pluginState.getAnsiCode() ||
            tempProps.colorName != pluginState.getColorName()

    override fun apply() {
        pluginState.updateEnabled(this.tempProps.enabled)
        pluginState.updateAnsiCode(this.tempProps.ansiCode)
        pluginState.updateColorName(this.tempProps.colorName)
    }

    override fun cancel() {
        this.tempProps.enabled = pluginState.getEnabled()
        this.tempProps.ansiCode = pluginState.getAnsiCode()
        this.tempProps.colorName = pluginState.getColorName()
    }

    override fun reset() {
        this.tempProps.enabled = pluginState.getEnabled()
        this.tempProps.ansiCode = pluginState.getAnsiCode()
        this.tempProps.colorName = pluginState.getColorName()
        settingPanel.reset()
    }
}