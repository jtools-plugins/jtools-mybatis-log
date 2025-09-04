package com.jtools.mybatislog

import com.intellij.ide.AppLifecycleListener

class JToolsMyBatisLogApplicationListener:AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        StarterJavaProgramPatcher.install()
    }
}