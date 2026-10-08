# AGENTS.md

## 适用范围

本文件适用于整个仓库。RediI 由故障复现框架、运行时、系统辅助类、基准资产和 63 个数据集用例组成。开始修改前先阅读 `PROJECT_STRUCTURE.md`。

## 模块边界

- 根 `pom.xml` 的 Maven reactor 只包含 `reditrt` 和 `redit`。
- `reditrt` 是被 AspectJ 编织进目标 JVM 程序的运行时客户端；保持轻量，不得反向依赖 `redit`。
- `redit` 是核心框架，包含部署 DSL、定义校验、工作区、代码编织、Docker 执行和事件服务。
- `helper` 是独立 Maven 工程，默认依赖 Maven Central 上的 `io.github.martylinzy:redit:0.1.0`，不会随根 reactor 自动构建。
- `Benchmark` 是故障注入与版本构建资产；`dataset` 是按 issue 组织的端到端复现用例。不要将它们当成根 reactor 的单元测试。

## 环境约束

- 生产目标为 Java 8；新代码不得使用 Java 9+ API 或语法。
- 核心编译需 Maven 3.x。确定性复现还需 Git LFS、AspectJ 1.8+、Docker 和 Linux 网络能力。
- Java 编织通过 `$ASPECTJ_HOME/bin/ajc` 执行；安装的 AspectJ 版本应与用例 POM 中的 `aspectjrt` 保持一致。
- 端到端用例优先在 Ubuntu/Linux、Java 8、Docker 24.0.5 环境运行。`Benchmark/README.md` 已明确记录 Docker 25.x/26.x 不兼容。macOS 只适合常规编译和非 Linux 依赖的检查。
- 仓库历史文档混用 RediI、RediT、Redit、Redil 和 RediB。除非任务明确是统一命名，不要批量改动 Maven 坐标、Java 包名、公开 API 或数据集路径。

## 实现规则

- 保持现有 `Deployment.Builder` / `Service.Builder` / `Node.Builder` 链式 DSL 的语义和返回类型。修改公开 DSL 时，同步检查实体、verifier、workspace、instrumentation、runtime 和数据集调用点。
- 事件模型变更必须联动检查 `dsl/events`、`RunSequenceVerifier`、`EventService`、`reditrt` 及 AspectJ 操作生成。
- Docker 运行时变更需考虑容器权限、`docker0`/事件服务地址、iptables/tc、时钟偏移和停机清理。不要在无 Linux 端到端证据时宣称这类修改已完整验证。
- 新增日志使用 SLF4J。异常优先使用现有领域异常，不要吞掉 Docker、工作区或编织错误。
- 运行 `ReditRunner` 的新测试必须在 `finally` 或 JUnit 清理阶段调用 `runner.stop()`。
- 文档源文件在 `docs/PageBuildSource/*.rst` 和 `docs/PageBuildSource/pages/*.rst`。不要手工修改 `docs/` 下的生成 HTML、`build/` 或 `_build/`。

## 验证命令

根 reactor 的默认检查：

```bash
mvn test
```

针对核心模块：

```bash
mvn -pl redit -am test
mvn -pl reditrt test
```

针对独立 helper：

```bash
mvn -f helper/pom.xml test
```

`helper` 该命令默认验证的是 Maven Central 上已发布的 `redit:0.1.0`。如果同时修改了核心框架和 helper，必须先将当前根 reactor 产物安装到本地 Maven 仓库，并明确处理根 POM 的 GPG 签名配置，再验证 helper。

现有 `redit/src/test/java` 中的 3 个 `*Test.java` 只包含 `main` 方法，`mvn test` 实际执行 0 个测试。不得将“BUILD SUCCESS”等同于已有行为覆盖；修改可纯单元验证的逻辑时，补充 JUnit 4 `@Test` 和断言。

单个数据集用例是独立 Maven 工程：

```bash
mvn -f dataset/<System>/Redit-<System>-<Issue>/pom.xml test
```

仅在已准备对应 tar 包、helper、AspectJ、Docker 和权限时运行该命令。结果中应记录用例路径、issue、Docker/Java/AspectJ 版本及是否复现预期差异。
数据集解析 `io.redit.helpers:helper:1.0-SNAPSHOT` 前，需先显式执行 `mvn -f helper/pom.xml install`。

## 高影响操作

- 不得为常规验证自动执行 `init.sh`。它会先删除 `Benchmark` 下匹配的 tar/zip，然后下载、解压和重打包大型资产。
- 不得在未说明的情况下运行 `reditWorkingDirDeleter.sh` 或 `dataset/reditWorkingDirDeleter.sh`；后者还会递归删除 `target` 目录。
- 不要提交 `.ReditWorkingDirectory/`、`target/`、临时解压目录、未要求的服务日志或新下载的大型二进制。
- 根 POM 在 `verify` 阶段绑定 GPG 签名。普通本地验证使用 `mvn test`；需要 `verify/install/deploy` 时先确认签名和发布目的。

## 交付要求

说明实际执行的命令、通过/失败/未执行的范围和环境限制。涉及 `Benchmark` 或 `dataset` 时，必须明确区分“编译通过”与“故障已复现”。
