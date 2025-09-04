package com.jtools.mybatislog

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import javax.swing.JComponent

class JToolsMybatisLogConfigurable(val project: Project):Configurable {

    val pluginState:PluginState = PluginState()

    init {
        cancel()
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "JToolsMybatisLog"

    override fun createComponent(): JComponent = SettingPanel(project, pluginState)

    override fun isModified(): Boolean = pluginState.enabled != PluginState.getInstance(project).enabled ||
            pluginState.ansiCode != PluginState.getInstance(project).ansiCode ||
            pluginState.colorName != PluginState.getInstance(project).colorName

    override fun apply(){
        var persistentState = PluginState.getInstance(project)
        persistentState.ansiCode = pluginState.ansiCode
        persistentState.colorName = pluginState.colorName
        persistentState.enabled = pluginState.enabled
    }

    override fun cancel() {
        var persistentState = PluginState.getInstance(project)
        pluginState.enabled = persistentState.enabled
        pluginState.colorName = persistentState.colorName
        pluginState.ansiCode = persistentState.ansiCode
    }
}