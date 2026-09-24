package com.lhstack.jtools.mybatis

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.extensions.Extensions
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

        private val javaProgramPatcher = StarterJavaProgramPatcher()

        /**
         * 由 jtools 宿主在安装本插件时调用。独立插件模式的 plugin.xml 扩展点不再使用。
         */
        fun registry() {
            javaProgramPatcher.register()
        }

        fun unRegistry() {
            javaProgramPatcher.unregister()
        }
    }

    /**
     * EP_NAME 在目标 SDK 中是 protected。companion 会编译成独立类,
     * 不能直接访问父类受保护字段,因此注册必须发生在子类实例方法里。
     */
    internal fun register() {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(EP_NAME)
        // 宿主运行时的 ExtensionPoint 没有 hasExtension,只能遍历已注册实例。
        if (extensionPoint.extensions.none { it === this }) {
            extensionPoint.registerExtension(this)
        }
    }

    internal fun unregister() {
        val extensionPoint = Extensions.getRootArea().getExtensionPoint(EP_NAME)
        extensionPoint.unregisterExtension(this)
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
        addModuleOpens(javaParameters)
        javaParameters.vmParametersList.add(buildAgentArgument(agent, state))
    }

    /**
     * JDK 9 及以上需要打开 agent 反射访问的模块,否则宿主应用在模块系统下无法启动。
     */
    private fun addModuleOpens(javaParameters: JavaParameters) {
        if (javaVersion(javaParameters) < 9) {
            return
        }
        javaParameters.vmParametersList.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
        javaParameters.vmParametersList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
        javaParameters.vmParametersList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
    }

    private fun javaVersion(javaParameters: JavaParameters): Int {
        val versionString = javaParameters.jdk?.versionString ?: return 8
        val version = versionString.replace(Regex("[^0-9.]"), "")
        return when {
            version.startsWith("1.8") -> 8
            version.startsWith("1.") -> version.substringAfter("1.").substringBefore(".").toIntOrNull() ?: 8
            else -> version.substringBefore(".").toIntOrNull() ?: 8
        }
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
