# Stream-AI 需求开发计划

> 基于架构文档 `docs/architecture.md` (v2.0) 和代码结构 `docs/code-structure.md` (v2.0) 拆解

---

## 1. 流程理解

### 核心流程 (根据用户输入更新)

```
用户输入提示词 → LLM理解意图 → 生成/优化规则 → 规则引擎
                                              ↓
源数据库 → CDC → 规则引擎(拦截) → 数据流处理 → 报告
               ↓                              ↑
          未命中 ──▶ LLM定期分析 ──▶ 新规则 ──┘
```

### 三层闭环

| 闭环 | 触发 | 流程 |
|------|------|------|
| **闭环1: 规则生成** | 用户输入 | 用户输入 → LLM理解 → 规则生成 → 规则引擎 |
| **闭环2: 主数据流转** | CDC数据 | CDC → 规则拦截 → 数据处理 → LLM分析 → 报告 |
| **闭环3: 规则优化** | 报告异常+用户输入 | 报告异常 + 用户想法 → LLM优化 → 规则引擎 |

---

## 2. 版本规划

### 2.1 MVP 版本 (v1.0) - 核心数据流转

**目标**: 实现规则引擎 + 数据流转 + 基础报告

```
Sprint 1-2: 核心控制层 (orchestrator) + 规则引擎 (ruleengine)
Sprint 3-4: 数据流处理 (pipeline) + 与规则引擎集成
Sprint 5:   报告生成 (report) + MVP 交付
```

### 2.2 智能版本 (v2.0) - LLM 集成

**目标**: 实现意图理解、规则生成、规则优化、定期分析

```
Sprint 6-7: LLM 客户端 + 意图理解
Sprint 8:   规则生成 (rulegen)
Sprint 9:   规则优化 (ruleopt)
Sprint 10:  定期分析 (scheduled) + 智能版本交付
```

### 2.3 交付版本 (v3.0) - 完整产品

**目标**: API接口、通知服务、产品化

```
Sprint 11: 通知服务 (notification)
Sprint 12: API 接口 (api)
Sprint 13: 产品化 + 测试 + 交付
```

---

## 3. Sprint 详细规划

### Sprint 1: 核心控制层 + 规则引擎核心接口

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-101 | Rule 抽象接口定义 | ruleengine | 3 | - | Rule 接口 + RuleContext + RuleResult |
| T-102 | RuleMatchResult 匹配结果 | ruleengine | 2 | T-101 | 匹配/未匹配/异常 |
| T-103 | Condition 条件接口 | ruleengine | 5 | T-101 | Condition 接口 + SimpleCondition |
| T-104 | Action 动作接口 | ruleengine | 3 | T-101 | Action 接口 + AlertAction + LogAction |
| T-105 | RuleEngine 接口定义 | ruleengine | 5 | T-102,T-103,T-104 | RuleEngine 接口 + 默认实现 |
| T-106 | RuleManager 规则管理 | ruleengine | 3 | T-105 | 规则增删改查能力 |
| T-107 | StreamOrchestrator 接口 | orchestrator | 5 | - | 核心编排器接口 |
| T-108 | FlowController 流程控制 | orchestrator | 5 | T-107,T-105 | 流程串联控制 |
| T-109 | 单元测试覆盖 | ruleengine | 5 | T-101~T-106 | 80% 覆盖率 |

**Sprint 目标**: 规则引擎核心接口 + 核心控制层基础

---

### Sprint 2: 内置规则实现 + 用户输入处理

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-201 | AbstractRule 基类 | ruleengine | 2 | T-101 | 规则基类 + 模板方法 |
| T-202 | ThresholdRule 阈值规则 | ruleengine | 8 | T-201 | 支持 > < >= <= == 条件 |
| T-203 | PatternRule 模式匹配 | ruleengine | 8 | T-201 | 正则表达式匹配 |
| T-204 | CorrelationRule 关联规则 | ruleengine | 13 | T-201 | 跨表关联分析 |
| T-205 | CompositeRule 组合规则 | ruleengine | 8 | T-201 | AND/OR 组合逻辑 |
| T-206 | UserInputHandler 接口 | orchestrator | 5 | T-108 | 用户输入处理接口 |
| T-207 | RuleLifecycleManager | orchestrator | 5 | T-106 | 规则生命周期管理 |
| T-208 | 单元测试覆盖 | ruleengine | 5 | T-201~T-205 | 85% 覆盖率 |

**Sprint 目标**: 5种内置规则实现 + 用户输入处理基础

---

### Sprint 3: Pipeline 核心接口 + 处理器

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-301 | Pipeline 接口定义 | pipeline | 5 | - | Pipeline 接口 + PipelineContext |
| T-302 | CdcProcessor 接口 | pipeline | 3 | T-301 | 处理器接口抽象 |
| T-303 | FilterProcessor 过滤 | pipeline | 8 | T-302 | 按条件过滤事件 |
| T-304 | TransformProcessor 转换 | pipeline | 8 | T-302 | 字段映射/转换 |
| T-305 | EnrichProcessor enrichment | pipeline | 8 | T-302 | 数据 enrichment |
| T-306 | RouterProcessor 路由 | pipeline | 5 | T-302 | 条件路由 |
| T-307 | PipelineMetrics 指标 | pipeline | 3 | T-301 | 处理统计指标 |

**Sprint 目标**: Pipeline 核心处理能力完成

---

### Sprint 4: Pipeline 缓冲 + 与规则引擎集成 + 报告基础

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-401 | EventBuffer 事件缓冲 | pipeline | 8 | T-301 | 内存缓冲 + 容量控制 |
| T-402 | SlidingWindow 滑动窗口 | pipeline | 8 | T-401 | 时间/数量窗口 |
| T-403 | Pipeline 与 RuleEngine 集成 | pipeline | 8 | T-105,T-301 | 拦截/未拦截分流 |
| T-404 | Pipeline 单元测试 | pipeline | 5 | T-301~T-403 | 85% 覆盖率 |
| T-405 | Report 报告模型 | report | 3 | - | 报告数据结构 |
| T-406 | ReportContext 上下文 | report | 3 | T-405 | 报告生成上下文 |
| T-407 | ReportType 报告类型 | report | 2 | T-405 | 实时/定时/按需 |
| T-408 | ReportConfig 报告配置 | report | 3 | T-407 | 是否生成配置 |

**Sprint 目标**: Pipeline 与规则引擎集成 + 报告基础

---

### Sprint 5: MVP 端到端联调 + 报告生成 + 交付

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-501 | MockCdcSource 与 Pipeline 集成 | datasource,pipeline | 5 | T-403 | 事件流转串联 |
| T-502 | 端到端数据流转测试 | integration | 8 | T-404 | 全链路测试通过 |
| T-503 | ReportFormatter 接口 | report | 3 | T-406 | 格式化器接口 |
| T-504 | JsonFormatter JSON | report | 5 | T-503 | JSON 格式输出 |
| T-505 | HtmlFormatter HTML | report | 5 | T-503 | HTML 格式输出 |
| T-506 | DefaultReportGenerator | report | 8 | T-504,T-505 | 报告生成器实现 |
| T-507 | 性能基准测试 | pipeline | 5 | T-502 | 毫秒级处理验证 |
| T-508 | MVP 版本打包 | build | 3 | T-507 | 可执行 JAR |
| T-509 | MVP 文档编写 | docs | 5 | T-508 | 使用文档 |

**Sprint 目标**: MVP 版本交付，可运行演示

---

### Sprint 6: LLM 客户端 + 意图理解核心

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-601 | LlmConfig LLM 配置 | llm | 3 | - | API Key/端点配置 |
| T-602 | Anthropic SDK 集成 | llm | 5 | T-601 | Claude API 调用封装 |
| T-603 | LlmClient 客户端封装 | llm | 8 | T-602 | 统一 LLM 调用接口 |
| T-604 | Intent 意图接口 | llm | 5 | T-603 | Intent 定义 + IntentType |
| T-605 | IntentParser 意图解析器 | llm | 8 | T-604 | NL → Intent 转换 |
| T-606 | IntentResolver 意图分解 | llm | 5 | T-605 | 意图拆分 + 路由 |
| T-607 | UserInputHandler 实现 | orchestrator | 8 | T-206,T-605 | 用户输入处理实现 |

**Sprint 目标**: LLM 客户端 + 意图理解完成

---

### Sprint 7: Skill 库 + 与 Orchestrator 集成

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-701 | Skill 抽象接口 | llm | 3 | T-603 | Skill 基类 |
| T-702 | SkillRegistry 注册中心 | llm | 5 | T-701 | Skill 注册/发现 |
| T-703 | SkillInvoker 调用器 | llm | 5 | T-702 | Skill 调度执行 |
| T-704 | MonitoringSkill 监控 | llm | 8 | T-703 | 监控技能实现 |
| T-705 | AnomalyDetectionSkill 异常 | llm | 13 | T-703 | 异常检测技能 |
| T-706 | ReconciliationSkill 对账 | llm | 8 | T-703 | 对账技能实现 |
| T-707 | AnalysisSkill 分析 | llm | 8 | T-703 | 分析技能实现 |
| T-708 | NotificationSkill 通知 | llm | 5 | T-703 | 通知技能实现 |
| T-709 | Orchestrator 集成 | orchestrator | 5 | T-607,T-703 | 与 LLM 集成 |

**Sprint 目标**: 5个核心 Skill + 与 Orchestrator 集成

---

### Sprint 8: 规则生成 (RuleGen)

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-801 | RuleTemplate 规则模板 | llm.rulegen | 5 | T-604 | 规则模板定义 |
| T-802 | RuleGenerator 接口 | llm.rulegen | 3 | T-801 | 生成器接口 |
| T-803 | LlmRuleGenerator 实现 | llm.rulegen | 13 | T-802,T-603 | LLM 驱动生成 |
| T-804 | RuleGeneratorContext | llm.rulegen | 3 | T-803 | 生成上下文 |
| T-805 | 规则生成 API 集成 | orchestrator | 5 | T-207,T-803 | 用户输入触发生成 |
| T-806 | 与 RuleEngine 集成 | ruleengine | 5 | T-803,T-106 | 生成后自动入库 |

**Sprint 目标**: 用户输入 → 规则生成 → 规则引擎闭环

---

### Sprint 9: 规则优化 (RuleOpt)

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-901 | AnalysisResult 分析结果 | llm.ruleopt | 3 | T-603 | 分析结果模型 |
| T-902 | OptimizationContext | llm.ruleopt | 3 | T-901 | 优化上下文 |
| T-903 | RuleOptimizer 接口 | llm.ruleopt | 3 | T-902 | 优化器接口 |
| T-904 | LlmRuleOptimizer 实现 | llm.ruleopt | 13 | T-903,T-603 | LLM 驱动优化 |
| T-905 | 规则优化触发 | orchestrator | 5 | T-501,T-904 | 报告异常触发优化 |
| T-906 | 用户输入触发优化 | orchestrator | 5 | T-207,T-904 | 用户想法触发优化 |

**Sprint 目标**: 报告异常 + 用户输入 → 规则优化闭环

---

### Sprint 10: 定期 LLM 分析 (Scheduled) + 智能版本交付

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-1001 | AnalysisContext 分析上下文 | llm.scheduled | 3 | T-603 | 分析上下文 |
| T-1002 | AnalysisTask 分析任务 | llm.scheduled | 3 | T-1001 | 分析任务定义 |
| T-1003 | SchedulerManager 调度管理 | orchestrator | 5 | - | 定时调度器 |
| T-1004 | ScheduledAnalyzer 定期分析器 | llm.scheduled | 13 | T-1002,T-603 | 未拦截数据分析 |
| T-1005 | 定期分析触发新规则 | llm.scheduled | 5 | T-1004,T-803 | 分析结果生成规则 |
| T-1006 | 智能版本端到端测试 | integration | 8 | T-805,T-906,T-1005 | 全链路测试 |
| T-1007 | 智能版本性能优化 | performance | 5 | T-1006 | 性能调优 |
| T-1008 | v2.0 版本打包发布 | release | 3 | T-1007 | 智能版本交付 |

**Sprint 目标**: v2.0 智能版本交付

---

### Sprint 11: 通知服务

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-1101 | NotificationConfig 通知配置 | notification | 3 | - | 邮件/开关配置 |
| T-1102 | NotificationService 接口 | notification | 3 | T-1101 | 通知服务接口 |
| T-1103 | EmailNotifier 邮件通知 | notification | 8 | T-1102 | SMTP 邮件发送 |
| T-1104 | WebhookNotifier Webhook | notification | 5 | T-1102 | HTTP Webhook |
| T-1105 | ReportScheduler 报告调度 | report | 5 | T-408 | 报告定时调度 |
| T-1106 | 通知与报告集成 | report,notification | 5 | T-506,T-1103 | 报告生成后触发通知 |

**Sprint 目标**: 通知服务完成，按需发送

---

### Sprint 12: API 接口

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-1201 | Spring Boot 集成 | api | 5 | - | Web 框架搭建 |
| T-1202 | StreamController 流控制 | api | 8 | T-1201 | 启动/停止/状态 |
| T-1203 | RuleController 规则管理 | api | 8 | T-106 | CRUD 接口 |
| T-1204 | ReportController 报告 | api | 5 | T-506 | 报告查询接口 |
| T-1205 | ChatController 对话 | api | 8 | T-607 | 意图对话接口 |
| T-1206 | AnalysisController 分析 | api | 5 | T-1004 | 分析查询接口 |
| T-1207 | DTO 请求/响应对象 | api | 5 | T-1202~T-1206 | 数据传输对象 |
| T-1208 | Service 服务层 | api | 8 | T-1207 | 业务逻辑封装 |
| T-1209 | API 文档 Swagger | api | 5 | T-1208 | API 文档生成 |

**Sprint 目标**: REST API 完成

---

### Sprint 13: 产品化 + 测试 + 交付

**任务拆解**:

| 任务ID | 任务名称 | 模块 | 预估 | 依赖 | 验收标准 |
|--------|----------|------|------|------|----------|
| T-1301 | 配置中心 | config | 5 | - | YAML 配置管理 |
| T-1302 | 健康检查 | api | 3 | T-1201 | Health Check 接口 |
| T-1303 | 监控指标 | metrics | 5 | T-307 | Prometheus 集成 |
| T-1304 | 全量集成测试 | test | 8 | T-1209 | 全链路测试 |
| T-1305 | 压力测试 | performance | 5 | T-1304 | 高并发验证 |
| T-1306 | 安全测试 | security | 5 | T-1305 | 渗透测试 |
| T-1307 | 用户文档 | docs | 8 | T-1306 | 使用手册 |
| T-1308 | 部署脚本 | devops | 5 | T-1307 | Docker/K8s 部署 |
| T-1309 | 最终验收测试 | test | 5 | T-1308 | UAT 通过 |
| T-1310 | v3.0 发布 | release | 3 | T-1309 | 正式版本发布 |

**Sprint 目标**: v3.0 正式版本交付

---

## 4. 关键里程碑

| 里程碑 | 版本 | Sprint | 交付物 |
|--------|------|--------|--------|
| M1 | v0.1 | Sprint 2 | 规则引擎核心 + 5种内置规则 + 用户输入处理 |
| M2 | v0.5 | Sprint 4 | Pipeline 完整 + 与规则引擎集成 + 报告基础 |
| M3 | v1.0 | Sprint 5 | **MVP 交付** - 端到端数据流转 + 规则拦截 |
| M4 | v1.5 | Sprint 7 | LLM 客户端 + 意图理解 + 5个 Skill |
| M5 | v2.0 | Sprint 10 | **智能版本交付** - 规则生成/优化 + 定期分析 |
| M6 | v3.0 | Sprint 13 | **正式版本交付** - 完整产品 |

---

## 5. 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| LLM API 响应延迟 | 高 | 异步处理 + 缓存 + 分层决策 |
| 规则生成质量 | 中 | 人工审核 + 模板约束 |
| 性能瓶颈 | 高 | 预Benchmark + 优化点识别 |
| 复杂场景支持 | 中 | 迭代完善 + 场景扩展 |

---

## 6. 总工作量估算

| 阶段 | Sprint 数 | 故事点合计 | 人月估算 (2人团队) |
|------|-----------|------------|---------------------|
| MVP (v1.0) | 5 | ~95 | 2.4 个月 |
| 智能 (v2.0) | 5 | ~100 | 2.5 个月 |
| 交付 (v3.0) | 3 | ~65 | 1.6 个月 |
| **总计** | **13** | **~260** | **~6.5 个月** |

---

## 7. 三层闭环依赖链

```
闭环1 (规则生成): api.ChatCtrl → orchestrator.UserInput → llm.IntentParser → llm.rulegen → ruleengine

闭环2 (主数据流转):
  datasource → ruleengine(拦截) → pipeline → llm.scheduled → ruleengine(新规则)
                              ↓(未拦截)
                            llm.scheduled → report → notification(按需)

闭环3 (规则优化): report(异常) + api.ChatCtrl → llm.ruleopt → ruleengine
```

---
*文档版本: v2.0*
*基于用户流程定义更新*
