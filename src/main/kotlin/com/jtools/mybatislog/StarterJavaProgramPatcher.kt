package com.jtools.mybatislog

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.application.ApplicationManager
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files

class StarterJavaProgramPatcher : JavaProgramPatcher() {


    companion object {

        private val javaProgramPatcher: JavaProgramPatcher = StarterJavaProgramPatcher()

        fun registry() {
            val jar =
                StarterJavaProgramPatcher::class.java.classLoader.getResourceAsStream("META-INF/agent.jar")
            jar?.use {
                val bytes = it.readAllBytes()
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
            val extensionPoint = ApplicationManager.getApplication().extensionArea.getExtensionPoint(EP_NAME)
            extensionPoint.registerExtension(javaProgramPatcher) {

            }
        }

        fun unRegistry() {
            val file = File(System.getProperty("user.home") + "/.jtools/jtools-mybatis-log/agent.jar")
            file.runCatching {
                FileUtils.delete(file)
            }
            val extensionPoint = ApplicationManager.getApplication().extensionArea.getExtensionPoint(EP_NAME)
            extensionPoint.unregisterExtension(StarterJavaProgramPatcher::class.java)
        }
    }

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        javaParameters.vmParametersList.add("-javaagent:${System.getProperty("user.home") + "/.jtools/jtools-mybatis-log/agent.jar"}")
    }
}