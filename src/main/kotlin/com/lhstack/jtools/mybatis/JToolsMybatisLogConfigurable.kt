package com.lhstack.jtools.mybatis

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

class TempProps {
    var enabled = true
    var ansiCode = "92"
    var colorName: String = "亮绿色"
    var configJsonPath:String = ""
    var configJsonValue:String = ""
}

class JToolsMybatisLogConfigurable(val project: Project) : Configurable {

    val pluginState: PluginState = PluginState.getInstance(project)

    val tempProps = TempProps()
    var settingPanel: SettingPanel
    init {
        tempProps.enabled = pluginState.getEnabled()
        tempProps.ansiCode = pluginState.getAnsiCode()
        tempProps.colorName = pluginState.getColorName()
        tempProps.configJsonPath = pluginState.getJsonConfigPath()
        tempProps.configJsonValue = pluginState.getJsonConfigValue()
        settingPanel = SettingPanel(project, tempProps)
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(settingPanel)
    }

    override fun getDisplayName(): String = "JToolsMybatisLog"
    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent = settingPanel

    override fun isModified(): Boolean = tempProps.enabled != pluginState.getEnabled() ||
            tempProps.ansiCode != pluginState.getAnsiCode() ||
            tempProps.colorName != pluginState.getColorName() || tempProps.configJsonValue != pluginState.getJsonConfigValue()

    override fun apply() {
        pluginState.updateEnabled(this.tempProps.enabled)
        pluginState.updateAnsiCode(this.tempProps.ansiCode)
        pluginState.updateColorName(this.tempProps.colorName)
        pluginState.updateJsonConfigPath(this.tempProps.configJsonPath)
        pluginState.updateJsonConfigValue(this.tempProps.configJsonValue)
    }

    fun cancel() {
        this.tempProps.enabled = pluginState.getEnabled()
        this.tempProps.ansiCode = pluginState.getAnsiCode()
        this.tempProps.colorName = pluginState.getColorName()
        this.tempProps.configJsonPath = pluginState.getJsonConfigPath()
        this.tempProps.configJsonValue = pluginState.getJsonConfigValue()
    }

    override fun reset() {
        this.tempProps.enabled = pluginState.getEnabled()
        this.tempProps.ansiCode = pluginState.getAnsiCode()
        this.tempProps.colorName = pluginState.getColorName()
        this.tempProps.configJsonPath = pluginState.getJsonConfigPath()
        this.tempProps.configJsonValue = pluginState.getJsonConfigValue()
        settingPanel.reset()
    }
}