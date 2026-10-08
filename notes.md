# Notes: RediI 项目分析

## Sources

- `pom.xml`、`redit/pom.xml`、`reditrt/pom.xml`、`helper/pom.xml`
- `redit/src/main/java`、`reditrt/src/main/java`、`helper/src/main/java`
- `redit/src/test/java`与 `dataset/*/*/src/main/java`
- `README.md`、`Benchmark/README.md`、`dataset/README.md`、`docs/PageBuildSource/pages/*.rst`
- `init.sh`、`reditWorkingDirDeleter.sh`

## Synthesized Findings

### 定位与模块
- RediI 包含分布式系统故障数据集和确定性复现工具 RediT。
- 根 Maven reactor 仅包含 `reditrt` 和 `redit`；`helper` 是独立 Maven 工程，不在根 `modules` 内。
- `reditrt` 是被编织到目标 Java 程序中的轻量运行时；`redit` 包含 DSL、校验、工作区、AspectJ 编织、Docker 执行和事件服务。
- `helper` 提供 ActiveMQ、Cassandra、HDFS、HBase、Kafka、RocketMQ、ZooKeeper 等测试辅助类。
- `Benchmark` 保存故障注入/修复资产、构建脚本与版本目录；`dataset` 保存按 issue 组织的 Maven/JUnit 复现用例。

### 主要运行链路
1. 用 `Deployment.Builder` 定义 service、node、内部事件、测试事件和 run sequence。
2. `ReditRunner.run()` 校验引用与事件序列，创建 `.ReditWorkingDirectory`。
3. `RunSequenceInstrumentationEngine` 生成 AspectJ 切面，`JavaInstrumentor` 调用 `$ASPECTJ_HOME/bin/ajc` 编织目标 jar/目录。
4. `SingleNodeRuntimeEngine` 通过 Spotify Docker Client 创建单机 Docker 网络和容器。
5. Jetty/Jersey 事件服务调度节点内 `reditrt` 上报的事件和测试中 `enforceOrder` 事件。
6. `LimitedRuntimeEngine` 对外提供节点启停、网络分区/延迟/丢包、时钟偏移、容器内命令和事件等待 API。

### 环境与风险
- 代码编译目标为 Java 8，文档也推荐 Java 8、Maven 3.x、AspectJ、Git LFS、Linux 和 root/Docker 权限。
- `Benchmark/README.md` 明确记录 Docker 24.0.5 可用，25.x/26.x 已知不兼容；当前机器为 macOS arm64、Docker 20.10.22、`ASPECTJ_HOME` 未设置，不适合作为端到端复现结果。
- `init.sh` 会先删除 `Benchmark` 下已有目标 tar/zip，再下载并重打包，属于高影响操作，不应在普通验证中自动执行。
- 清理脚本会递归删除 `.ReditWorkingDirectory`；`dataset/reditWorkingDirDeleter.sh` 还会删除所有 `target`。
- 根 POM 在 `verify` 阶段绑定 GPG 签名；常规本地校验优先使用 `mvn test`，不把 `mvn verify/install` 当成无条件命令。

### 实际验证
- `mvn test`：成功编译根 reactor 的 `reditrt` 和 `redit`，但 Surefire 报告为 0 tests。
- `redit/src/test/java` 下的 3 个类都是手工 `main` 检查，不是 JUnit 测试方法。
- `mvn -f helper/pom.xml test`：编译成功，无测试；该 POM 从 Maven Central 下载已发布的 `io.github.martylinzy:redit:0.1.0`。
- 两次构建都警告 `maven-compiler-plugin` 未锁定版本、未设置项目编码（核心 `redit` 资源/部分模块）及 Java 8 `source/target` 在新 JDK 上的兼容警告。
