plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.lhstack.jtools.mybatis"
version = "1.1.1"


repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
}

intellij {
    version.set("2017.1")
    type.set("IC") // Target IDE Platform
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.apiVersion = "1.9"
    }

    patchPluginXml {
        sinceBuild.set("171")
        untilBuild.set("265.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

/**
 * 校验随插件分发的 agent.jar 是否落后于 agent 源码。
 *
 * agent 模块不在 Gradle 构建内(独立 Maven 项目，产物手工拷贝到
 * src/main/resources/META-INF/agent.jar)。漏掉这一步时 Gradle 构建照样成功，
 * 但插件行为仍是旧 agent，属于静默失效，因此在打包前显式拦住。
 *
 * 判定依据是修改时间而非内容: jar 内为 shade 后的字节码，无法与源文件直接比对。
 * 时间戳能覆盖「改了已有文件」和「新增文件」两种漏打包情形。
 */
val verifyAgentJar by tasks.registering {
    group = "verification"
    description = "校验 META-INF/agent.jar 是否比 agent 源码新"

    val agentSourceRoot = layout.projectDirectory.dir("agent/src/main/java")
    val bundledAgentJar = layout.projectDirectory.file("src/main/resources/META-INF/agent.jar")

    inputs.dir(agentSourceRoot)
    inputs.file(bundledAgentJar)

    doLast {
        val jarFile = bundledAgentJar.asFile
        if (!jarFile.isFile) {
            throw GradleException("缺少 ${jarFile.path}，请先构建 agent 模块并拷贝产物")
        }

        val sourceRoot = agentSourceRoot.asFile
        val sources = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()
        if (sources.isEmpty()) {
            throw GradleException("未找到 agent 源码: ${sourceRoot.path}")
        }

        val staleSources = sources
            .filter { it.lastModified() > jarFile.lastModified() }
            .map { it.relativeTo(sourceRoot).path }
            .sorted()

        if (staleSources.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("agent.jar 落后于 agent 源码，以下文件在打包后被修改:")
                    staleSources.forEach { appendLine("  - $it") }
                    appendLine("请重新构建 agent 并拷贝产物:")
                    appendLine("  cd agent && mvn clean package")
                    append("  cp target/agent-1.0-SNAPSHOT.jar ../src/main/resources/META-INF/agent.jar")
                }
            )
        }
        logger.lifecycle("agent.jar 校验通过，覆盖 ${sources.size} 个源文件")
    }
}

tasks.named("processResources") {
    dependsOn(verifyAgentJar)
}
