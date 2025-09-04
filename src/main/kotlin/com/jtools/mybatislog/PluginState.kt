package com.jtools.mybatislog

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(value = [Level.PROJECT])
@State(name = "data", storages = [Storage("JtoolsMybatisLog.xml")])
class PluginState() : PersistentStateComponent<PluginState> {

    var enabled: Boolean = true

    var ansiCode:String = "92"

    var colorName:String = "亮绿色"

    override fun getState(): PluginState = this

    override fun loadState(state: PluginState) {
        this.enabled = state.enabled
        this.ansiCode = state.ansiCode
        this.colorName = state.colorName
    }

    companion object {
        fun getInstance(project: Project): PluginState = project.getService(PluginState::class.java)
    }

}