package com.jtools.mybatislog


import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.lhstack.tools.plugins.IPlugin
import javax.swing.Icon
import javax.swing.JComponent

class PluginImpl : IPlugin {
    companion object {
        val componentCache = mutableMapOf<String, JComponent>()
    }

    override fun install() {
        StarterJavaProgramPatcher.registry()
    }

    override fun unInstall() {
        StarterJavaProgramPatcher.unRegistry()
    }


    override fun createPanel(project: Project): JComponent = componentCache.computeIfAbsent(project.locationHash) {
        val state = PluginState.getInstance(project)
        val tempProps = TempProps()
        tempProps.ansiCode = state.getAnsiCode()
        tempProps.enabled = state.getEnabled()
        tempProps.colorName = state.getColorName()
        SettingPanel(project, tempProps) {
            state.updateEnabled(it.enabled)
            state.updateAnsiCode(it.ansiCode)
            state.updateColorName(it.colorName)
        }
    }

    override fun closePanel(project: Project, pluginPanel: JComponent) {
        super.closePanel(project, pluginPanel)
        componentCache.remove(project.locationHash)
    }

    override fun installRestart(): Boolean = false

    override fun support(jToolsVersion: Int): Boolean = jToolsVersion >= 103

    override fun pluginIcon(): Icon? = IconLoader.findIcon("icons/mybatis.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon? = IconLoader.findIcon("icons/mybatis_tab.svg", PluginImpl::class.java)

    override fun pluginName(): String = "jtools-mybatis-log"

    override fun pluginDesc(): String = "jtools-mybatis-log"

    override fun pluginVersion(): String = "v1.0.5"
}