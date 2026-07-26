package com.lhstack.jtools.mybatis

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 插件配置读写。
 *
 * 颜色、开关等轻量配置存在 IDE 的 PropertiesComponent 中;
 * 排除包与格式化选项存在独立的 properties 文件里,因为该文件需要被
 * 目标进程中的 agent 直接读取。
 */
class PluginState(val properties: PropertiesComponent, val project: Project) {

    companion object {
        const val PROPERTIES_PREFIX = "JTools.Mybatis.Log"

        fun getInstance(project: Project): PluginState =
            PluginState(PropertiesComponent.getInstance(project), project)
    }

    fun getEnabled(): Boolean = properties.getBoolean("$PROPERTIES_PREFIX.enabled", true)

    fun getAnsiCode(): String = properties.getValue("$PROPERTIES_PREFIX.ansiCode", "92")

    fun getColorName(): String = properties.getValue("$PROPERTIES_PREFIX.colorName", "亮绿色")

    /**
     * 只解析路径,不创建目录或文件: 该方法会被 isModified 高频调用,
     * getter 不应带副作用。文件在写入时按需创建。
     */
    fun getJsonConfigPath(): String =
        properties.getValue("$PROPERTIES_PREFIX.jsonConfigPath", defaultJsonConfigPath())

    private fun defaultJsonConfigPath(): String = File(
        System.getProperty("user.home"),
        ".jtools/jtools-mybatis-log/${project.name}/config.properties"
    ).absolutePath

    /**
     * 配置文件尚未创建时返回空串(首次使用的正常状态);
     * 文件存在但读取失败属于真实故障,异常向上抛出。
     */
    fun getJsonConfigValue(): String {
        val file = File(getJsonConfigPath())
        if (!file.isFile) {
            return ""
        }
        return file.readText(StandardCharsets.UTF_8)
    }

    fun updateEnabled(enabled: Boolean) {
        properties.setValue("$PROPERTIES_PREFIX.enabled", enabled.toString())
    }

    fun updateAnsiCode(ansiCode: String) {
        properties.setValue("$PROPERTIES_PREFIX.ansiCode", ansiCode)
    }

    fun updateColorName(colorName: String) {
        properties.setValue("$PROPERTIES_PREFIX.colorName", colorName)
    }

    fun updateJsonConfigPath(jsonConfigPath: String) {
        properties.setValue("$PROPERTIES_PREFIX.jsonConfigPath", jsonConfigPath)
    }

    /**
     * 写入失败时抛出 IOException,由调用方转成用户可见的错误。
     * 静默失败会让用户以为配置已保存,而 agent 实际读到的是旧配置。
     */
    fun updateJsonConfigValue(value: String) {
        val file = File(getJsonConfigPath())
        file.parentFile?.mkdirs()
        file.writeText(value, StandardCharsets.UTF_8)
    }
}
