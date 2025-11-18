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

    val agentPath = "${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar"
    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration is RunConfiguration) {
            val jar =
                StarterJavaProgramPatcher::class.java.classLoader.getResourceAsStream("META-INF/agent.jar")
                    ?: Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/agent.jar")
            //如果md5校验失败,则重新写入agent
            jar?.use {
                val bytes = it.readBytes()
                val agentFile = File(agentPath)
                if (!agentFile.exists() || (DigestUtils.md5Hex(Files.readAllBytes(Path(agentPath))) != DigestUtils.md5Hex(
                        bytes
                    ))
                ) {
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
                javaParameters.vmParametersList.add(
                    "-javaagent:${agentPath}=${state.getAnsiCode()},${
                        Base64.getEncoder().encodeToString(
                            state.getJsonConfigPath().toByteArray(
                                StandardCharsets.UTF_8
                            )
                        )
                    }"
                )
            }

//            javaParameters.vmParametersList.add("-javaagent:D:\\projects\\java\\jtools-mybatis-log\\agent\\target\\agent-1.0-SNAPSHOT.jar=${enabled},${color}")
        }

    }
}