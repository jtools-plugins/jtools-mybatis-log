package com.jtools.mybatislog

import com.intellij.ide.AppLifecycleListener
import java.io.File
import java.nio.file.Files

class JToolsMyBatisLogApplicationListener:AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        val jar =
            StarterJavaProgramPatcher::class.java.classLoader.getResourceAsStream("META-INF/agent.jar")?:Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/agent.jar")
        jar?.use {
            val bytes = it.readBytes()
            val dir = System.getProperty("user.home") + "/.jtools/jtools-mybatis-log"
            File(dir).apply {
                if (!this.exists()) {
                    this.mkdirs()
                }
                File(this, "agent.jar").apply {
                    if (this.exists()) {
                        val existBytes = Files.readAllBytes(this.toPath())
                        if (existBytes.size != bytes.size) {
                            Files.write(this.toPath(), bytes)
                        }
                    } else {
                        Files.write(this.toPath(), bytes)
                    }
                }
            }
            bytes
        }
    }
}