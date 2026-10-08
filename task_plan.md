# Task Plan: RediI 项目分析与协作文档整理

## Goal
基于实际代码整理一份可执行的 `AGENTS.md` 和项目结构文档，并验证关键命令与结论。

## Phases
- [x] Phase 1: 建立计划与分析边界
- [x] Phase 2: 检查代码、依赖、配置、测试与运行入口
- [x] Phase 3: 编写 `AGENTS.md` 与 `PROJECT_STRUCTURE.md`
- [x] Phase 4: 校验文档准确性并交付

## Key Questions
1. 项目的技术栈、运行链路和核心模块是什么？
2. 开发、测试、构建和数据/模型资产有哪些前置条件？
3. 代码修改时有哪些必须遵守的边界、风险和验证要求？

## Decisions Made
- 文档名使用通用的大写 `AGENTS.md`，供编码 Agent 自动发现。
- 项目结构单独写入 `PROJECT_STRUCTURE.md`，避免 `AGENTS.md` 承载过多背景信息。
- 核心修改的默认验证命令为 `mvn test`；结果为构建成功，但现有 3 个 `*Test.java` 只有 `main` 方法，Surefire 实际执行 0 个测试。
- `helper` 独立使用 `mvn -f helper/pom.xml test` 验证，当前构建成功但无测试，且它默认解析 Maven Central 上的 `redit:0.1.0`。

## Errors Encountered
- `git lfs status --porcelain=v1` 不被当前 Git LFS 支持；改用 `git lfs status --porcelain` 后成功，当前无 LFS 变更。

## Status
**Completed** - `AGENTS.md` 与 `PROJECT_STRUCTURE.md` 已编写，关键路径、统计、Markdown 格式和构建结果已复核。
