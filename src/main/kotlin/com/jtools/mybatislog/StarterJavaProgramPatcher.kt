package com.jtools.mybatislog

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.application.ApplicationManager
import org.apache.commons.codec.binary.Base64
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
                        Files.write(this.toPath(), bytes)
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
            if(state.getEnabled()){
                // JDK 9+ module system compatibility
                val javaVersion = getJavaVersion(javaParameters)
                if (javaVersion >= 9) {
                    javaParameters.vmParametersList.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    javaParameters.vmParametersList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                    javaParameters.vmParametersList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                }
                javaParameters.vmParametersList.add(
                    "-javaagent:${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar=${state.getAnsiCode()},${java.util.Base64.getEncoder().encodeToString(state.getJsonConfigPath().toByteArray(
                        StandardCharsets.UTF_8))}"
                )
            }
        }
    }

    private fun getJavaVersion(javaParameters: JavaParameters): Int {
        return try {
            val jdk = javaParameters.jdk
            val versionString = jdk?.versionString ?: return 8
            val version = versionString.replace(Regex("[^0-9.]"), "")
            when {
                version.startsWith("1.8") -> 8
                version.startsWith("1.") -> version.substringAfter("1.").substringBefore(".").toIntOrNull() ?: 8
                else -> version.substringBefore(".").toIntOrNull() ?: 8
            }
        } catch (e: Exception) {
            8
        }
    }
}