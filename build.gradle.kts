import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    id("org.jetbrains.intellij.platform") version "2.7.2"
}

group = "com.jtools.mybatislog"
version = "1.0.5"


repositories {
    intellijPlatform {
        defaultRepositories()
    }
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
}

dependencies {
    implementation(files("C:/Users/lhstack/.jtools/sdk/sdk.jar"))
    testImplementation(kotlin("test"))
    intellijPlatform{
        intellijIdeaCommunity("2022.3")
        bundledPlugin("com.intellij.java")
    }
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }
    withType<JavaExec> {
        jvmArgs("-Dfile.encoding=UTF-8")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions{
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }

}