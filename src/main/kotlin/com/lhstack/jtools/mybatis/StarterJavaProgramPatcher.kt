package com.lhstack.jtools.mybatis

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

class StarterJavaProgramPatcher : JavaProgramPatcher() {

    private val agentPath = "${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar"

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration is RunConfiguration) {
            val jar = StarterJavaProgramPatcher::class.java.classLoader.getResourceAsStream("META-INF/agent.jar")
                ?: Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/agent.jar")
            
            // 如果md5校验失败,则重新写入agent
            jar?.use {
                val bytes = it.readBytes()
                val agentFile = File(agentPath)
                if (!agentFile.exists() || (DigestUtils.md5Hex(Files.readAllBytes(Path(agentPath))) != DigestUtils.md5Hex(bytes))) {
                    val dir = System.getProperty("user.home") + "/.jtools/jtools-mybatis-log"
                    File(dir).apply {
                        if (!this.exists()) {
                            this.mkdirs()
                        }
                        File(this, "agent.jar").apply {
                            Files.write(this.toPath(), bytes)
                        }
                    }
                }
            }
            
            val state = PluginState.getInstance(configuration.project)
            if (state.getEnabled()) {
                // JDK 9+ module system compatibility
                val javaVersion = getJavaVersion(javaParameters)
                if (javaVersion >= 9) {
                    javaParameters.vmParametersList.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    javaParameters.vmParametersList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                }
                javaParameters.vmParametersList.add(
                    "-javaagent:${agentPath}=${state.getAnsiCode()},${
                        Base64.getEncoder().encodeToString(state.getJsonConfigPath().toByteArray(StandardCharsets.UTF_8))
                    }"
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
