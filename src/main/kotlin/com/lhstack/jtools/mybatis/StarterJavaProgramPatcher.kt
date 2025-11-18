package com.lhstack.jtools.mybatis

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path

class StarterJavaProgramPatcher : JavaProgramPatcher() {

    val agentPath = "${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.jar"

    val agentSignaturePath = "${System.getProperty("user.home")}/.jtools/jtools-mybatis-log/agent.signature"

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration is RunConfiguration) {
            val agentSignature = File(agentSignaturePath).let {
                if(it.isFile && it.exists()) it.readText(StandardCharsets.UTF_8) else ""
            }
            val memorySignatureBytes = StarterJavaProgramPatcher::class.java.classLoader.getResourceAsStream("META-INF/agent.signature")?:Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/agent.signature")
            val memorySignature = memorySignatureBytes?.use {
                it.readBytes().toString(StandardCharsets.UTF_8)
            } ?: UUID.randomUUID().toString()
            //如果md5校验失败,则重新写入agent
            if(memorySignature != agentSignature){
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
                            Files.write(Path(agentSignaturePath),memorySignature.toByteArray(StandardCharsets.UTF_8))
                        }
                    }
                    bytes
                }
            }
            val state = PluginState.getInstance(configuration.project)
            if(state.getEnabled()){
                javaParameters.vmParametersList.add(
                    "-javaagent:${agentPath}=${state.getAnsiCode()},${java.util.Base64.getEncoder().encodeToString(state.getJsonConfigPath().toByteArray(
                        StandardCharsets.UTF_8))}"
                )
            }

//            javaParameters.vmParametersList.add("-javaagent:D:\\projects\\java\\jtools-mybatis-log\\agent\\target\\agent-1.0-SNAPSHOT.jar=${enabled},${color}")
        }

    }
}