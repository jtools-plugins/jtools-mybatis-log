package com.lhstack.jtools.mybatis

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.lhstack.jtools.mybatis.PluginState
import com.lhstack.jtools.mybatis.SettingPanel
import com.lhstack.jtools.mybatis.StarterJavaProgramPatcher
import com.lhstack.jtools.mybatis.TempProps
import com.lhstack.tools.plugins.IPlugin
import javax.swing.Icon
import javax.swing.JComponent

/**
 * jtools 宿主插件的加载入口。类名由 META-INF/ToolsPlugin.txt 指定。
 */
class PluginImpl : IPlugin {

    companion object {
        private val componentCache = mutableMapOf<String, JComponent>()
        private val disposer = mutableMapOf<String, Disposable>()
    }

    override fun install() {
        StarterJavaProgramPatcher.registry()
    }

    override fun unInstall() {
        StarterJavaProgramPatcher.unRegistry()
    }

    override fun createPanel(project: Project): JComponent {
        val key = project.locationHash
        return componentCache[key] ?: createSettingPanel(project).also { componentCache[key] = it }
    }

    private fun createSettingPanel(project: Project): SettingPanel {
        val state = PluginState.getInstance(project)
        val tempProps = TempProps().apply {
            enabled = state.getEnabled()
            ansiCode = state.getAnsiCode()
            colorName = state.getColorName()
            configJsonPath = state.getJsonConfigPath()
            configJsonValue = state.getJsonConfigValue()
        }
        return SettingPanel(project, tempProps) {
            state.updateEnabled(it.enabled)
            state.updateAnsiCode(it.ansiCode)
            state.updateColorName(it.colorName)
            state.updateJsonConfigPath(it.configJsonPath)
            state.updateJsonConfigValue(it.configJsonValue)
        }.also {
            disposer[project.locationHash] = it
        }
    }

    override fun closeProject(project: Project) {
        componentCache.remove(project.locationHash)
        disposer.remove(project.locationHash)?.let { Disposer.dispose(it) }
    }

    override fun installRestart(): Boolean = false

    override fun support(jToolsVersion: Int): Boolean = jToolsVersion >= 103

    override fun pluginIcon(): Icon? =
        IconLoader.findIcon("icons/mybatis.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon? =
        IconLoader.findIcon("icons/mybatis_tab.svg", PluginImpl::class.java)

    override fun pluginName(): String = "jtools-mybatis-log"

    override fun pluginDesc(): String = "jtools-mybatis-log"

    override fun pluginVersion(): String = "v1.1.3"
}
