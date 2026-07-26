package com.lhstack.jtools.mybatis

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

class TempProps {
    var enabled = true
    var ansiCode = "92"
    var colorName: String = "亮绿色"
    var configJsonPath: String = ""
    var configJsonValue: String = ""
}

class JToolsMybatisLogConfigurable(val project: Project) : Configurable {

    private val pluginState: PluginState = PluginState.getInstance(project)

    private val tempProps = TempProps()

    private var settingPanel: SettingPanel? = null

    /** 上次从磁盘读到的配置内容,作为 isModified 的比较基线,避免在 UI 线程反复读文件。 */
    private var savedConfigValue: String = ""

    /**
     * 面板在 createComponent 时才创建: Configurable 可能只被实例化用于搜索索引,
     * 此时不应提前构造 UI 组件。
     */
    override fun createComponent(): JComponent {
        loadIntoTempProps()
        return SettingPanel(project, tempProps).also { settingPanel = it }
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        settingPanel?.let { Disposer.dispose(it) }
        settingPanel = null
    }

    override fun getDisplayName(): String = "JToolsMybatisLog"

    override fun getHelpTopic(): String? = null

    override fun isModified(): Boolean = tempProps.enabled != pluginState.getEnabled() ||
            tempProps.ansiCode != pluginState.getAnsiCode() ||
            tempProps.colorName != pluginState.getColorName() ||
            tempProps.configJsonPath != pluginState.getJsonConfigPath() ||
            tempProps.configJsonValue != savedConfigValue

    /**
     * 配置文件写入失败时抛 ConfigurationException,由 IDE 展示给用户。
     * 静默忽略会让用户以为已保存,而 agent 下次仍读到旧配置。
     */
    override fun apply() {
        pluginState.updateEnabled(tempProps.enabled)
        pluginState.updateAnsiCode(tempProps.ansiCode)
        pluginState.updateColorName(tempProps.colorName)
        pluginState.updateJsonConfigPath(tempProps.configJsonPath)
        try {
            pluginState.updateJsonConfigValue(tempProps.configJsonValue)
            savedConfigValue = tempProps.configJsonValue
        } catch (e: Exception) {
            throw ConfigurationException("保存配置文件失败: ${pluginState.getJsonConfigPath()}, ${e.message}")
        }
    }

    override fun reset() {
        loadIntoTempProps()
        settingPanel?.reset()
    }

    private fun loadIntoTempProps() {
        tempProps.enabled = pluginState.getEnabled()
        tempProps.ansiCode = pluginState.getAnsiCode()
        tempProps.colorName = pluginState.getColorName()
        tempProps.configJsonPath = pluginState.getJsonConfigPath()
        savedConfigValue = readConfigValue()
        tempProps.configJsonValue = savedConfigValue
    }

    /**
     * 读取失败不应阻塞设置页打开,退化为空配置并提示,不掩盖异常内容。
     */
    private fun readConfigValue(): String = try {
        pluginState.getJsonConfigValue()
    } catch (e: Exception) {
        Notifier.warn(project, "读取配置文件失败: ${pluginState.getJsonConfigPath()}, ${e.message}")
        ""
    }
}
