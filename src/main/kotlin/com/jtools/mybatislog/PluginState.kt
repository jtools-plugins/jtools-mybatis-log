package com.jtools.mybatislog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

class PluginState(val properties: PropertiesComponent) {

    companion object {
        const val PROPERTIES_PREFIX = "JTools.Mybatis.Log"
        fun getInstance(project: Project): PluginState {
            val instance = PropertiesComponent.getInstance(project)
            val pluginState = PluginState(instance)
            return pluginState
        }
    }
    fun getEnabled():Boolean = properties.getBoolean("${PROPERTIES_PREFIX}.enabled", true)
    fun getAnsiCode(): String = properties.getValue("${PROPERTIES_PREFIX}.ansiCode","92")
    fun getColorName(): String = properties.getValue("${PROPERTIES_PREFIX}.colorName","亮绿色")

    fun updateEnabled(enabled:Boolean){
        properties.setValue("${PROPERTIES_PREFIX}.enabled", enabled.toString())
    }

    fun updateAnsiCode(ansiCode:String){
        properties.setValue("${PROPERTIES_PREFIX}.ansiCode", ansiCode)
    }

    fun updateColorName(colorName:String){
        properties.setValue("${PROPERTIES_PREFIX}.colorName", colorName)
    }

}