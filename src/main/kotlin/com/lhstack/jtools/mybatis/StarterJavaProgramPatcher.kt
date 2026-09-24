package com.lhstack.jtools.mybatis

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

/**
 * 为目标进程注入 agent。
 *
 * agent.jar 打包在插件资源里,首次运行或内容变化时释放到用户目录,
 * 因为 -javaagent 需要文件系统路径。
 */
class StarterJavaProgramPatcher : JavaProgramPatcher() {

    companion object {
        private const val AGENT_RESOURCE = "META-INF/agent.jar"
        private const val AGENT_DIR = ".jtools/jtools-mybatis-log"
        private const val AGENT_FILE_NAME = "agent.jar"
    }

    private val agentDir: File
        get() = File(System.getProperty("user.home"), AGENT_DIR)

    private val agentFile: File
        get() = File(agentDir, AGENT_FILE_NAME)

    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
        if (configuration !is RunConfiguration) {
            return
        }
        val state = PluginState.getInstance(configuration.project)
        if (!state.getEnabled()) {
            return
        }
        val agent = prepareAgentJar(configuration) ?: return
        javaParameters.vmParametersList.add(buildAgentArgument(agent, state))
    }

    /**
     * @return 可用的 agent 文件;释放失败时返回 null 并提示,不阻断应用启动
     */
    private fun prepareAgentJar(configuration: RunConfiguration): File? {
        return try {
            val bytes = readAgentResource()
            val target = agentFile
            if (!target.isFile || !contentMatches(target, bytes)) {
                writeAtomically(target, bytes)
            }
            target
        } catch (e: Exception) {
            Notifier.warn(configuration.project, "释放 agent.jar 失败,本次运行不会打印 SQL 日志: ${e.message}")
            null
        }
    }

    private fun readAgentResource(): ByteArray {
        val loader = StarterJavaProgramPatcher::class.java.classLoader
        val stream = loader.getResourceAsStream(AGENT_RESOURCE)
            ?: throw IOException("插件资源中缺少 $AGENT_RESOURCE")
        return stream.use { it.readBytes() }
    }

    private fun contentMatches(file: File, expected: ByteArray): Boolean =
        md5(file.readBytes()).contentEquals(md5(expected))

    private fun md5(bytes: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(bytes)

    /**
     * 先写临时文件再原子移动: 多个项目同时启动时,直接写目标文件可能
     * 让另一个进程读到不完整的 jar。
     */
    private fun writeAtomically(target: File, bytes: ByteArray) {
        val dir = target.parentFile
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw IOException("无法创建目录 ${dir.absolutePath}")
        }
        val temp = File.createTempFile(AGENT_FILE_NAME, ".tmp", dir)
        try {
            Files.write(temp.toPath(), bytes)
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    private fun buildAgentArgument(agent: File, state: PluginState): String {
        val encodedConfigPath = Base64.getEncoder()
            .encodeToString(state.getJsonConfigPath().toByteArray(StandardCharsets.UTF_8))
        return "-javaagent:${agent.absolutePath}=${state.getAnsiCode()},$encodedConfigPath"
    }
}
