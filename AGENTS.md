# AGENTS.md

本文件记录 `jtools-mybatis-log` 的工程约定，供后续任务作为项目级约束使用。所有结论均来自项目文件、构建配置与版本控制元数据；无法确认的内容已在最后一节标注。

## 项目概述

IntelliJ 平台插件，用于在控制台打印 MyBatis / MyBatis-Plus 的完整 SQL（参数已回填）与执行耗时。

实现方式为「IDE 插件 + Java Agent」两段式：

- 插件侧通过 `java.programPatcher` 扩展点，在运行/调试 Java 配置时注入 `-javaagent`。
- Agent 侧用 Javassist 改写 `org.apache.ibatis.session.Configuration#newExecutor` 与
  `com.baomidou.mybatisplus.core.MybatisConfiguration#newExecutor`，把返回的 `Executor` 包装为
  `ExecutorWrapper`，在委托前后生成 SQL 文本并计时输出。

插件 ID：`com.jtools.mybatis.log.jtools-mybatis-log`；当前版本 `1.1.0`（`build.gradle.kts`）。

## 工程环境与主要工具

| 项 | 值 | 证据 |
| --- | --- | --- |
| 插件侧语言 | Kotlin（JVM target 1.8，apiVersion 1.9） | `build.gradle.kts` |
| Agent 侧语言 | Java 8 | `agent/pom.xml` |
| 插件构建 | Gradle 8.6 + `org.jetbrains.intellij` 1.17.4 | `gradle/wrapper/gradle-wrapper.properties`、`build.gradle.kts` |
| Agent 构建 | Maven + `maven-shade-plugin` | `agent/pom.xml` |
| 目标平台 | IC 2017.1，`sinceBuild=171`，`untilBuild=261.*` | `build.gradle.kts`、`plugin.xml` |
| 依赖仓库 | mavenLocal → 阿里云 public → mavenCentral | `build.gradle.kts`、`settings.gradle.kts`、`agent/pom.xml` |
| 编码 | 全部 UTF-8 | `.idea/encodings.xml`、`agent/pom.xml` |
| Kotlin 代码风格 | official | `gradle.properties` |

两个构建体系相互独立：`settings.gradle.kts` 未包含 `agent` 模块，Gradle 侧也没有拷贝 agent 产物的任务。

## 目录与模块结构

```
├── build.gradle.kts                  # 插件构建
├── settings.gradle.kts               # 仅包含根项目
├── src/main/kotlin/com/lhstack/jtools/mybatis/
│   ├── Const.kt                      # ANSI 码 ↔ 颜色名 ↔ Swing Color 映射
│   ├── PluginState.kt                # 配置读写（PropertiesComponent + 外部 properties 文件）
│   ├── JToolsMybatisLogConfigurable.kt  # Settings 页入口 + TempProps 编辑态
│   ├── SettingPanel.kt               # 设置面板 UI
│   ├── MultiLanguageTextField.kt     # LanguageTextField 封装
│   └── StarterJavaProgramPatcher.kt  # 释放 agent.jar 并注入 -javaagent
├── src/main/resources/META-INF/
│   ├── plugin.xml
│   └── agent.jar                     # 预构建的 agent 产物，纳入版本控制
└── agent/src/main/java/com/lhstack/jtools/mybatis/
    ├── JtoolsAgent.java              # premain/agentmain + ClassFileTransformer
    ├── ExecutorWrapper.java          # Executor 代理，生成并打印 SQL
    ├── MockPreparedStatement.java    # 假 PreparedStatement，用于收集参数字面量
    └── AntPathMatcher.java           # Spring AntPathMatcher 的内联副本，用于排除匹配
```

`agent/target/`、`build/`、`out/` 为构建产物，分析与修改时应排除。

## 分层架构与依赖方向

```
SettingPanel ──► TempProps ──► JToolsMybatisLogConfigurable ──► PluginState ──► PropertiesComponent
                                                                          └──► ~/.jtools/... config.properties
StarterJavaProgramPatcher ──► PluginState（读取） ──► JavaParameters（写入 -javaagent）
                                    │
                       agent 参数：ansiCode + Base64(配置文件路径)
                                    ▼
JtoolsAgent（读 properties） ──► ExecutorWrapper ──► MockPreparedStatement / AntPathMatcher
```

约束：

- 插件侧与 Agent 侧不共享代码，唯一契约是 `-javaagent` 参数格式（`ansiCode,base64ConfigPath`）与配置文件的 key（`excludePackages`、`sqlFormatType`、`sqlFormatEnable`）。改动任一侧都必须同步另一侧。
- Agent 运行在用户应用的 JVM 中，对 `mybatis`、`mybatis-plus`、`pagehelper`、`slf4j` 均为 `optional` 依赖（`agent/pom.xml`），编译期可见但运行期不保证存在。
- Agent 的第三方依赖通过 shade relocate 到 `com.jtools.mybatislog.shade.*`，避免污染宿主应用；`org.springframework`、`com.baomidou`、`org.mybatis`、`org.slf4j`、`com.github.pagehelper` 被显式排除，不打进 agent.jar。

## 构建、测试和验证方式

- 插件：`./gradlew build`、`./gradlew buildPlugin`；`patchPluginXml` 覆盖 since/until build；`signPlugin`、`publishPlugin` 从环境变量读取凭据。
- Agent：在 `agent/` 下执行 Maven `package`，shade 后手工放置到 `src/main/resources/META-INF/agent.jar`。构建脚本中没有自动化这一步。
- 测试：仅声明了 `kotlin-test` 依赖，项目中不存在 `src/test` 或 `agent/src/test`，无任何自动化测试，也没有 CI 配置。验证方式为手工运行插件并观察控制台输出。

## 项目编码约定

- 包名统一 `com.lhstack.jtools.mybatis`（插件与 agent 同名包，不同源集）。
- Kotlin 侧偏好表达式体函数（`fun getEnabled(): Boolean = ...`）、`apply`/`also` 构建 Swing 组件、字符串模板拼接 key。
- 配置 key 统一前缀 `JTools.Mybatis.Log`，集中在 `PluginState` 中定义。
- UI 文案中英双语并列（`"添加包 (Add Package)"`），日志与标签使用中文。
- Java 侧（agent）字段多为 `private final`，构造函数注入；日志前缀统一 `[jtools-mybatis-log]`。
- `AntPathMatcher` 是外部代码的内联副本，保留原有 Tab 缩进与 Javadoc，不按本项目风格改写。

## 错误处理约定

当前代码的实际做法（描述现状，不代表推荐）：

- Agent 侧对宿主应用采取「绝不影响启动」策略：`premain`、`transform`、`enhance` 均以 `catch (Throwable)` 收口，失败时打印到 `System.err` 并返回 `null` 表示放弃增强。
- `ExecutorWrapper.genSql` 整体包在 `try/catch (Throwable)` 中，失败返回空字符串；分页相关的可选依赖访问使用 `catch (Throwable ignore)` 静默跳过。
- 日志输出对 slf4j 调用失败回退到 `System.out.printf`。
- 插件侧 `PluginState` 的文件读写使用 `catch (_: Exception) {}` 静默吞掉，`SettingPanel` 使用 `e.printStackTrace()`。

注意：静默吞异常与默认值兜底在本仓库中已大量存在，新增代码不应扩大这种做法；配置读写失败应向用户暴露。

## 版本控制信息

- Git，主分支 `master`，无 CI 配置。
- 提交者身份：`lhstack <lhstack@foxmail.com>`（绝大多数提交），另有少量历史提交来自另一邮箱。
- 提交信息风格由早期中文短句（“包名调整”、“版本更新”）逐步转为 Conventional Commits 前缀（`feat:`、`fix:`、`chore:`）。
- 存在同一变更内容重复提交多次的历史（如 v1.0.8、v1.1.0 的说明连续出现 2~3 次）。
- `src/main/resources/META-INF/agent.jar` 作为二进制产物纳入版本控制，agent 源码改动需要同时提交重新构建的 jar。
- 发布流程：`build.gradle.kts` 的 `version`、`plugin.xml` 的 `change-notes`、`README.md` 的更新日志三处需同步。

## 有证据支持的用户编码习惯

基于 `lhstack` 名下的提交内容：

- 面向 IDE 平台的低版本兼容优先，宁可放弃新 API 也要保住 `sinceBuild=171`（提交“全版本支持”、“修复 Idea 2017.1 版本兼容性问题”）。
- 手写 Swing 布局（`VerticalLayout` / `BorderLayout` / `FlowLayout` 嵌套 `JPanel`），不使用 Kotlin UI DSL。
- 配置采用「IDE PropertiesComponent 存元信息 + 用户目录 properties 文件存内容」的双通道方案，便于 agent 直接读取。
- 对宿主应用的侵入保持谨慎：可选依赖全部反射式/防御式访问，异常一律不外抛。
- 版本说明维护在 `README.md` 与 `plugin.xml` 的 `change-notes` 两处，按 `feat:` / `fix:` 分条列出。

## 当前无法确认的事项

- 未确认：agent.jar 的构建与拷贝是否存在未纳入版本控制的本地脚本。
- 未确认：`ToolsPlugin.txt` 中 `com.jtools.mybatislog.PluginImpl` 的用途，该类在本仓库中不存在，疑为其他 jtools 宿主插件的加载约定。
- 未确认：`untilBuild=261.*` 是否经过实际高版本 IDE 验证。
- 未确认：本机无可用 JDK，`./gradlew` 与 Maven 构建均未在当前环境验证过。
- 未确认：`MultiLanguageTextField` 与 `SettingPanel.configJsonPathField` 目前无引用者，是历史遗留还是为后续功能预留。
