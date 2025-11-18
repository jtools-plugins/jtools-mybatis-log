package com.jtools.mybatislog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class PluginState(val properties: PropertiesComponent,val project: Project) {

    companion object {
        const val PROPERTIES_PREFIX = "JTools.Mybatis.Log"
        fun getInstance(project: Project): PluginState {
            val instance = PropertiesComponent.getInstance(project)
            val pluginState = PluginState(instance,project)
            return pluginState
        }
    }
    fun getEnabled():Boolean = properties.getBoolean("${PROPERTIES_PREFIX}.enabled", true)
    fun getAnsiCode(): String = properties.getValue("${PROPERTIES_PREFIX}.ansiCode","92")
    fun getColorName(): String = properties.getValue("${PROPERTIES_PREFIX}.colorName","亮绿色")
    fun getJsonConfigPath() : String {
        val path = properties.getValue(
            "${PROPERTIES_PREFIX}.jsonConfigPath",
            "${System.getProperty("user.home")}\\.jtools\\jtools-mybatis-log\\${project.name}\\config.properties"
        )
        File(path).also {
            if(!it.exists()) {
                it.parentFile.mkdirs()
                it.createNewFile()
            }
        }
        return path
    }
    fun getJsonConfigValue() : String = File(getJsonConfigPath()).let {
        if(it.exists() && it.isFile){
            it.readText(StandardCharsets.UTF_8)
        }else {
            ""
        }
    }

    fun updateEnabled(enabled:Boolean){
        properties.setValue("${PROPERTIES_PREFIX}.enabled", enabled.toString())
    }

    fun updateAnsiCode(ansiCode:String){
        properties.setValue("${PROPERTIES_PREFIX}.ansiCode", ansiCode)
    }

    fun updateColorName(colorName:String){
        properties.setValue("${PROPERTIES_PREFIX}.colorName", colorName)
    }

    fun updateJsonConfigPath(jsonConfigPath:String){
        properties.setValue("${PROPERTIES_PREFIX}.jsonConfigPath", jsonConfigPath)
    }

    fun updateJsonConfigValue(value:String){
        FileOutputStream(getJsonConfigPath()).use {
            it.write(value.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }
}