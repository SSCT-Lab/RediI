# RediI 项目结构与代码分析

## 项目定位

RediI 是面向分布式系统故障确定性复现的 Java 框架。仓库同时保存框架源码、运行时、常见分布式系统辅助类、故障注入基准和可执行复现用例。

```text
.
├── pom.xml                         # Redit-parent，Java 8，仅聚合 reditrt + redit
├── reditrt/                        # 被编织进目标 JVM 的事件运行时
├── redit/                          # DSL、校验、工作区、编织、Docker 和事件调度
├── helper/                         # 各分布式系统的测试辅助类，独立 Maven 工程
├── Benchmark/                      # 故障版本、修复版本、注入代码和构建脚本
├── dataset/                        # 63 个按 issue 组织的 Maven/JUnit 复现用例
├── docs/                           # Sphinx 源文件与已生成站点
├── init.sh                        # 下载并重打包 Benchmark 所需系统发行包
├── reditWorkingDirDeleter.sh      # 清理 .ReditWorkingDirectory
└── README.md                       # 项目概述
```

## 模块依赖

```text
dataset 用例
   ├──> helper ──> Maven Central 中的 redit:0.1.0
   ├──> redit
   └──> reditrt

redit ──> reditrt
reditrt ──> JDK HTTP/JSON 通信逻辑

Benchmark 生成的目标 tar ──> dataset/RediHelper 的 Deployment.applicationPath()
```

`helper` 和 `dataset` 没有被根 `pom.xml` 聚合。因此根目录 `mvn test` 只验证 `reditrt` 和 `redit`，不会编译辅助类或执行任何数据集用例。

## 核心代码导航

| 路径 | 职责 | 关键类 |
| --- | --- | --- |
| `redit/src/main/java/io/redit/ReditRunner.java` | 框架总入口和生命周期 | `ReditRunner` |
| `redit/.../dsl/entities` | 服务、节点、路径、端口和部署的链式 DSL | `Deployment`, `Service`, `Node` |
| `redit/.../dsl/events` | 测试事件和节点内部事件模型 | `TestCaseEvent`, `StackTraceEvent`, `SchedulingEvent` |
| `redit/.../verification` | 部署引用、run sequence 语法与调度操作校验 | `InternalReferencesVerifier`, `RunSequenceVerifier` |
| `redit/.../workspace` | 创建 `.ReditWorkingDirectory`，复制/解压节点资产和日志目录 | `WorkspaceManager`, `NodeWorkspace` |
| `redit/.../instrumentation` | 将 DSL 事件转换为插桩定义 | `RunSequenceInstrumentationEngine` |
| `redit/.../instrumentation/runseq/java` | 生成 AspectJ 切面并调用 `ajc` 编织 | `AspectGenerator`, `JavaInstrumentor` |
| `redit/.../execution` | 对外运行 API、事件服务、网络分区/延迟/丢包 | `RuntimeEngine`, `EventServer`, `EventService`, `NetPart`, `NetOp` |
| `redit/.../execution/single_node` | Spotify Docker Client 驱动的单机多容器实现 | `SingleNodeRuntimeEngine`, `DockerNetworkManager` |
| `reditrt/src/main/java/io/redit/rt` | 目标 JVM 中的堆栈匹配、阻塞和事件上报 | `Redit`, `StackMatcher` |
| `helper/src/main/java/io/redit/helpers` | ActiveMQ、Cassandra、HDFS、HBase、Kafka、RocketMQ、ZooKeeper 等启停/健康检查 | `*Helper` |

## 运行链路

1. 用例通过 `Deployment.builder()` 定义 service、node、内部事件、测试事件与 run sequence。`Service` 是节点模板，`Node` 可覆盖启停命令、路径、环境变量和端口。
2. `ReditRunner.run()` 依次执行引用校验、run sequence 校验和调度操作校验。
3. `WorkspaceManager` 在当前目录创建 `.ReditWorkingDirectory`，为每个节点准备根目录、应用资产、解压内容、日志和 libfaketime。
4. `RunSequenceInstrumentationEngine` 把内部事件转换为编织定义。Java/Scala 路径由 `JavaInstrumentor` 调用 `$ASPECTJ_HOME/bin/ajc`，把 `reditrt` 调用编织进目标 jar 或 classes 目录。
5. `RuntimeEngine.getRuntimeEngine()` 当前固定返回 `SingleNodeRuntimeEngine`。它创建 Docker 网络、节点容器、挂载和端口映射。
6. 框架启动 Jetty/Jersey `EventServer`，把事件服务 IP/端口通过 `REDIT_EVENT_SERVER_*` 环境变量传入容器。
7. 被编织的目标程序使用 `reditrt.Redit` 上报堆栈事件、阻塞/解除阻塞或触发 GC；测试线程通过 `runner.runtime().enforceOrder()` 参与同一 run sequence。
8. `LimitedRuntimeEngine` 同时对用例暴露节点启停、网络分区、延迟/丢包、时钟偏移、容器内命令和事件等待 API。
9. 用例完成后 `runner.stop()` 停止容器、事件服务和文件共享服务；shutdown hook 是异常退出的最后保障。

## Benchmark 与 dataset

`Benchmark` 覆盖 ActiveMQ、Cassandra、Hadoop/HDFS、HBase、Kafka、RocketMQ 和 ZooKeeper。典型版本目录包含：

```text
Benchmark/<System>/<Version>/
├── buggy/          # 故障实现片段
├── fixed/          # 修复实现片段
├── inject.c        # 补丁/故障注入逻辑
├── FaultSeed.h     # issue 开关
├── build.sh        # 构建和重打包
└── <system>.tar.gz # 用例实际挂载的目标包，可能需 init.sh 生成
```

`dataset` 目前有 63 个案例：ActiveMQ 10、Cassandra 11、HDFS 9、HBase 7、Kafka 9、RocketMQ 8、ZooKeeper 9。典型用例结构为：

```text
dataset/<System>/Redit-<System>-<Issue>/
├── pom.xml
├── docker/Dockerfile
├── conf/
├── logs/                         # 已保存的故障/修复结果
└── src/main/java/.../
    ├── ReditHelper.java            # Deployment 与节点启动定义
    └── SampleTest.java             # JUnit 4 复现逻辑
```

这些 POM 把 `src/main/java` 设置为 `testSourceDirectory`，因此 `SampleTest` 会由 Surefire 执行。用例常依赖固定相对路径、目标发行包名、Docker 镜像、服务端口和旧版本客户端库；移动目录或统一依赖版本会直接改变复现语义。

## 文档与生成物

- Sphinx 源码：`docs/PageBuildSource/index.rst`、`docs/PageBuildSource/pages/*.rst`、`docs/PageBuildSource/conf.py`。
- Sphinx 构建命令：`make -C docs/PageBuildSource html`，需要 `sphinx-build` 和 `sphinx_rtd_theme`。
- `docs/`、`docs/PageBuildSource/build/` 和 `docs/PageBuildSource/_build/` 包含已生成 HTML/索引/中间产物。内容修改应先改 RST 源文件，再明确决定是否重建并提交生成站点。

## 当前工程现状

- `mvn test` 可成功编译 `reditrt` 和 `redit`；`mvn -f helper/pom.xml test` 也可成功编译 helper。
- `redit/src/test/java` 中 3 个类都是 `main` 形式的手工检查，当前 Surefire 结果为 0 tests。根模块缺少有断言的自动化单元测试。
- 仓库没有 Maven Wrapper、CI 配置、EditorConfig、Checkstyle 或 Spotless 配置。
- Maven 构建存在未锁定 `maven-compiler-plugin` 版本、部分编码未声明、新 JDK 下 Java 8 `source/target` 警告等可重现问题。
- 根 POM 在 `verify` 阶段绑定了 GPG 签名，所以 `mvn verify` 不是无条件的本地快速校验命令。
- 运行时实现与 Docker/Linux 网络、文件权限、root 能力和固定系统版本高度耦合；编译成功不代表数据集故障已复现。
