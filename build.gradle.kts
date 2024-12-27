plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.jtools.mybatislog"
version = "v1.0.1"


repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
}

intellij {
    version.set("2022.3")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("com.intellij.java", "org.jetbrains.plugins.yaml", "org.intellij.groovy","org.jetbrains.kotlin"))
}
dependencies {
    implementation(files("C:/Users/lhstack/.jtools/sdk/sdk.jar"))
    testImplementation(kotlin("test"))
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
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }

}
kotlin {
    jvmToolchain(17)
}