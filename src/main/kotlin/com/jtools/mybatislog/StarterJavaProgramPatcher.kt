package com.jtools.mybatislog

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.application.ApplicationManager
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files

class StarterJavaProgramPatcher : JavaProgramPatcher() {


    companion object {

        private val javaProgramPatcher: JavaProgramPatcher = StarterJavaProgramPatcher()

        fun install() {
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

        fun registry() {
            install()
            val extensionPoint = ApplicationManager.getApplication().extensionArea.getExtensionPoint(EP_NAME)
            extensionPoint.registerExtension(javaProgramPatcher) {

            }
        }

        fun unRegistry() {
            val extensionPoint = ApplicationManager.getApplication().extensionArea.getExtensionPoint(EP_NAME)
            extensionPoint.unregisterExtension(StarterJavaProgramPatcher::class.java)
        }
    }

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration is RunConfiguration) {
            val file = File("${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar")
            if(!file.exists()) {
                install()
            }
            val state = PluginState.getInstance(configuration.project)
            javaParameters.vmParametersList.add(
                "-javaagent:${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar=${state.getEnabled()},${state.getAnsiCode()}"
            )
//            javaParameters.vmParametersList.add("-javaagent:D:\\projects\\java\\jtools-mybatis-log\\agent\\target\\agent-1.0-SNAPSHOT.jar=${enabled},${color}")
        }

    }
}