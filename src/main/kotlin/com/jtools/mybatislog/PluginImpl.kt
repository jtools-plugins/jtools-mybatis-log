package com.jtools.mybatislog

import com.intellij.openapi.util.IconLoader
import com.lhstack.tools.plugins.IPlugin
import com.lhstack.tools.plugins.PluginType
import javax.swing.Icon

class PluginImpl : IPlugin {

    override fun install() {
        StarterJavaProgramPatcher.registry()
    }

    override fun unInstall() {
        StarterJavaProgramPatcher.unRegistry()
    }

    override fun pluginType(): PluginType = PluginType.JAVA_NON_UI

    override fun support(jToolsVersion: Int): Boolean = jToolsVersion >= 103

    override fun pluginIcon(): Icon? = IconLoader.findIcon("icons/mybatis.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon? = IconLoader.findIcon("icons/mybatis_tab.svg", PluginImpl::class.java)

    override fun pluginName(): String = "jtools-mybatis-log"

    override fun pluginDesc(): String = "jtools-mybatis-log"

    override fun pluginVersion(): String = "v1.0.0"
}