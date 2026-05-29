# Stream-AI 代码结构设计

> 基于架构文档 `docs/architecture.md` (v2.0) 设计

## 1. 代码结构总览

```
com/yashandb/streamai/
├── StreamAiApplication.java              # 主入口
│
├── datasource/                          # [已存在] CDC 数据源层
│   ├── CdcSource.java                   # CDC 数据源接口
│   ├── CdcEvent.java                    # CDC 事件抽象
│   ├── MockCdcSource.java               # Mock 数据源实现
│   ├── MockSceneType.java               # 场景类型枚举
│   ├── SourceMetrics.java               # 数据源指标
│   ├── TableMetrics.java                # 表级指标
│   ├── event/                           # CDC 事件类型
│   │   ├── CdcBeginEvent.java           # 事务开始事件
│   │   ├── CdcCommitEvent.java          # 事务提交事件
│   │   ├── CdcDdlEvent.java             # DDL 变更事件
│   │   ├── CdcDmlEvent.java             # DML 变更事件
│   │   └── CdcChunkEvent.java           # 大数据块事件
│   ├── mock/                            # Mock 事件生成器
│   │   ├── MockEventGenerator.java      # 事件生成器接口
│   │   ├── EcommerceEventGenerator.java
│   │   ├── FinanceRiskEventGenerator.java
│   │   ├── FinancialTickEventGenerator.java
│   │   ├── IotManufacturingEventGenerator.java
│   │   ├── TelecomReconEventGenerator.java
│   │   ├── TpccEventGenerator.java
│   │   └── TrafficProfile.java
│   └── pojo/                            # POJO 对象
│       ├── CdcEventType.java
│       ├── CdcOperationType.java
│       ├── ColumnValue.java
│       ├── LogPosition.java
│       ├── TableInfo.java
│       └── TransactionInfo.java
│
├── ruleengine/                          # [新增] 规则引擎层
│   ├── RuleEngine.java                  # 规则引擎核心接口
│   ├── DefaultRuleEngine.java           # 默认规则引擎实现
│   ├── Rule.java                        # 规则抽象
│   ├── RuleContext.java                 # 规则执行上下文
│   ├── RuleResult.java                  # 规则执行结果
│   ├── RuleManager.java                 # 规则管理器(增删改查)
│   ├── RuleMatchResult.java             # 规则匹配结果
│   ├── rule/                            # 内置规则实现
│   │   ├── AbstractRule.java            # 规则基类
│   │   ├── ThresholdRule.java           # 阈值规则
│   │   ├── PatternRule.java             # 模式匹配规则
│   │   ├── CorrelationRule.java         # 关联分析规则
│   │   └── CompositeRule.java           # 组合规则
│   ├── condition/                       # 条件定义
│   │   ├── Condition.java              # 条件接口
│   │   ├── SimpleCondition.java        # 简单条件
│   │   ├── CompoundCondition.java      # 复合条件
│   │   └── ConditionBuilder.java       # 条件构建器
│   └── action/                          # 动作定义
│       ├── Action.java                 # 动作接口
│       ├── AlertAction.java            # 告警动作
│       ├── LogAction.java              # 日志动作
│       └── CallbackAction.java         # 回调动作
│
├── pipeline/                            # [新增] 数据流处理层
│   ├── Pipeline.java                    # Pipeline 接口
│   ├── DefaultPipeline.java             # 默认 Pipeline 实现
│   ├── PipelineContext.java             # Pipeline 上下文
│   ├── PipelineMetrics.java             # Pipeline 指标
│   ├── processor/                       # 处理器
│   │   ├── CdcProcessor.java            # CDC 数据处理器接口
│   │   ├── FilterProcessor.java        # 过滤处理器
│   │   ├── TransformProcessor.java     # 转换处理器
│   │   ├── EnrichProcessor.java        #  enrichment 处理器
│   │   └── RouterProcessor.java        # 路由处理器
│   └── buffer/                          # 缓冲组件
│       ├── EventBuffer.java            # 事件缓冲
│       └── SlidingWindow.java          # 滑动窗口
│
├── orchestrator/                        # [新增] 核心控制层
│   ├── StreamOrchestrator.java          # 核心编排器(主流程控制)
│   ├── UserInputHandler.java            # 用户输入处理器
│   ├── FlowController.java              # 流程控制器
│   ├── SchedulerManager.java            # 定时调度管理器
│   └── lifecycle/                       # 生命周期管理
│       └── RuleLifecycleManager.java   # 规则生命周期
│
├── agent/                               # [新增] Agent 管控层（大脑核心）
│   ├── BaseAgent.java                   # Agent 基接口
│   ├── BrainAgent.java                  # 大脑 Agent（核心管控中心）
│   ├── AgentRegistry.java              # Agent 注册表（子 Agent 管理）
│   ├── ToolRegistry.java               # 工具注册表（Agent 工具能力）
│   ├── ReactLoop.java                  # React 循环分析引擎
│   ├── IntentRecognitionAgent.java     # 意图识别 Agent
│   ├── RuleGenerationAgent.java        # 规则生成 Agent
│   ├── RuleOptimizationAgent.java       # 规则优化 Agent
│   ├── DataAnalysisAgent.java          # 数据分析 Agent
│   ├── ActionExecutionAgent.java       # 行动执行 Agent
│   └── tools/                          # 工具实现
│       ├── ActionTool.java             # 工具基接口
│       ├── LogTool.java                # 日志工具
│       ├── AlertTool.java              # 告警工具
│       ├── NotifyTool.java            # 通知工具
│       └── HtmlReportTool.java         # 报告生成工具
│
├── llm/                                 # [新增] LLM 大脑层
│   ├── LlmClient.java                   # LLM 客户端封装
│   ├── LlmConfig.java                   # LLM 配置
│   ├── Intent Understanding/           # 意图理解
│   │   ├── IntentParser.java           # 意图解析器
│   │   ├── Intent.java                # 意图定义
│   │   ├── IntentResolver.java         # 意图分解
│   │   └── IntentType.java             # 意图类型枚举
│   ├── skill/                           # Skill 库
│   │   ├── Skill.java                  # Skill 抽象
│   │   ├── SkillRegistry.java          # Skill 注册中心
│   │   ├── SkillInvoker.java           # Skill 调用器
│   │   ├── monitoring/                 # 监控 Skill
│   │   │   └── MonitoringSkill.java
│   │   ├── anomaly/                   # 异常 Skill
│   │   │   └── AnomalyDetectionSkill.java
│   │   ├── reconciliation/             # 对账 Skill
│   │   │   └── ReconciliationSkill.java
│   │   ├── analysis/                  # 分析 Skill
│   │   │   └── AnalysisSkill.java
│   │   └── notification/               # 通知 Skill
│   │       └── NotificationSkill.java
│   ├── rulegen/                         # 规则生成
│   │   ├── RuleGenerator.java          # 规则生成器接口
│   │   ├── LlmRuleGenerator.java      # LLM 驱动规则生成
│   │   ├── RuleTemplate.java          # 规则模板
│   │   └── RuleGeneratorContext.java  # 生成上下文
│   ├── ruleopt/                         # 规则优化
│   │   ├── RuleOptimizer.java         # 规则优化器接口
│   │   ├── LlmRuleOptimizer.java      # LLM 驱动规则优化
│   │   ├── OptimizationContext.java   # 优化上下文
│   │   └── AnalysisResult.java        # 分析结果
│   └── scheduled/                       # 定期LLM分析
│       ├── ScheduledAnalyzer.java      # 定期分析器
│       ├── AnalysisContext.java        # 分析上下文
│       └── AnalysisTask.java           # 分析任务
│
├── report/                              # [新增] 报告生成层
│   ├── ReportGenerator.java            # 报告生成器接口
│   ├── DefaultReportGenerator.java    # 默认报告生成
│   ├── Report.java                     # 报告模型
│   ├── ReportContext.java              # 报告上下文
│   ├── ReportType.java                 # 报告类型枚举
│   ├── ReportConfig.java               # 报告配置(是否生成/发送)
│   ├── ReportScheduler.java            # 报告定时调度
│   └── formatter/                      # 格式化器
│       ├── ReportFormatter.java
│       ├── JsonFormatter.java
│       ├── HtmlFormatter.java
│       └── PdfFormatter.java
│
├── notification/                        # [新增] 通知层
│   ├── NotificationService.java       # 通知服务接口
│   ├── EmailNotifier.java             # 邮件通知
│   ├── WebhookNotifier.java           # Webhook 通知
│   └── NotificationConfig.java        # 通知配置
│
├── api/                                 # [新增] API 层
│   ├── StreamAiApplication.java       # Spring Boot 入口
│   ├── controller/                     # 控制器
│   │   ├── StreamController.java       # 流控制
│   │   ├── RuleController.java        # 规则管理
│   │   ├── ReportController.java      # 报告查询
│   │   ├── ChatController.java       # 对话接口(用户输入)
│   │   └── AnalysisController.java   # 分析查询
│   ├── dto/                            # 数据传输对象
│   │   ├── request/                    # 请求 DTO
│   │   │   ├── CreateRuleRequest.java
│   │   │   ├── QueryRequest.java
│   │   │   ├── SubscribeRequest.java
│   │   │   ├── UserInputRequest.java  # 用户输入请求
│   │   │   └── AnalysisRequest.java   # 分析请求
│   │   └── response/                   # 响应 DTO
│   │       ├── RuleResponse.java
│   │       ├── ReportResponse.java
│   │       ├── StreamResponse.java
│   │       └── IntentResponse.java    # 意图理解响应
│   └── service/                        # 服务层
│       ├── StreamService.java
│       ├── RuleService.java
│       ├── ReportService.java
│       └── ChatService.java
│
├── exception/                          # [已存在] 异常体系
│   ├── SaiException.java
│   ├── ErrorCode.java
│   └── ... (其他业务异常)
│
└── config/                              # [新增] 配置层
    ├── AppConfig.java                  # 应用配置
    ├── LlmConfig.java                  # LLM 配置
    ├── RuleEngineConfig.java          # 规则引擎配置
    ├── PipelineConfig.java            # Pipeline 配置
    ├── DatasourceConfig.java          # 数据源配置
    └── NotificationConfig.java        # 通知配置
```

## 2. 模块职责说明

### 2.1 核心流程模块映射

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              用户输入流程（大脑管控）                                │
│                                                                                     │
│   ┌──────────┐     ┌──────────────────────────────────────────────────────┐     │
│   │  API     │────▶│                    BrainAgent (大脑)                     │     │
│   │ ChatCtrl │     │  ┌─────────────────────────────────────────────────┐ │     │
│   └──────────┘     │  │  React 循环分析                                      │ │     │
│                    │  │  ① 理解用户意图  ② 分析需求  ③ 决策是否需要子Agent   │ │     │
│                    │  │  ④ 如有疑问可询问用户  ⑤ 调用子Agent执行任务          │ │     │
│                    │  └─────────────────────────────────────────────────┘ │     │
│                    │                         │                                   │     │
│                    │         ┌───────────────┼───────────────┐                  │     │
│                    │         ▼               ▼               ▼                  │     │
│                    │  ┌──────────┐   ┌──────────┐   ┌──────────────┐            │     │
│                    │  │IntentRec │   │RuleGen   │   │RuleOptimize  │            │     │
│                    │  │Agent    │   │Agent    │   │Agent         │            │     │
│                    │  │(意图识别)│   │(规则生成)│   │(规则优化)    │            │     │
│                    │  └────┬─────┘   └────┬─────┘   └──────┬───────┘            │     │
│                    │       │               │               │                    │     │
│                    │       └───────────────┼───────────────┘                    │     │
│                    │                       ▼                                    │     │
│                    │              ┌──────────────────┐                         │     │
│                    │              │  ToolRegistry    │                         │     │
│                    │              │  (工具能力调用)   │                         │     │
│                    │              └────────┬─────────┘                         │     │
│                    │                       │                                    │     │
│                    └───────────────────────┼────────────────────────────────────┘     │
│                                            ▼                                        │
│                                    ┌────────────┐                                  │
│                                    │ ruleengine │                                  │
│                                    │  规则入库   │                                  │
│                                    └────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              主数据流转（大脑监控）                                  │
│                                                                                     │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌────────┐         │
│   │datasource│──▶│ruleengine│──▶│ pipeline │──▶│  report │──▶│notify  │         │
│   │  CDC    │   │  实时拦截 │   │ 数据处理 │   │ 按需生成 │   │ 按需   │         │
│   └──────────┘   └────┬─────┘   └──────────┘   └──────────┘   └────────┘         │
│                       │                                                       │
│                       │ 未命中                                                   │
│                       ▼                                                       │
│              ┌──────────────────────────────────────┐                          │
│              │         BrainAgent (大脑)            │                          │
│              │  ┌─────────────────────────────────┐  │                          │
│              │  │  定期分析报告 + 动作决策        │  │                          │
│              │  │  • 分析未拦截数据模式           │  │                          │
│              │  │  • 决定是否生成新规则           │  │                          │
│              │  │  • 决定是否优化现有规则         │  │                          │
│              │  └─────────────────────────────────┘  │                          │
│              └──────────────────┬───────────────────┘                          │
│                                 │                                               │
│                                 ▼                                               │
│                        ┌──────────────┐                                        │
│                        │ ruleengine    │                                        │
│                        │ 新规则/优化   │                                        │
│                        └──────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         规则优化流程（大脑决策）                                     │
│                                                                                     │
│   ┌──────────┐   ┌──────────────────────────────────────────────────────────┐    │
│   │ 报告异常 │   │                    BrainAgent (大脑)                       │    │
│   │         │   │  ┌────────────────────────────────────────────────────┐  │    │
│   └────┬─────┘   │  │  判断用户是否有新想法                                 │  │    │
│        │         │  │  • 有 → 调用 RuleOptimizationAgent 优化规则         │  │    │
│        ▼         │  │  • 无 → 仅记录异常，等待定期分析                     │  │    │
│   ┌──────────┐   │  └────────────────────────────────────────────────────┘  │    │
│   │ 用户输入  │   └─────────────────────────┬───────────────────────────────┘    │
│   │ 原始想法  │                             │                                    │
│   └────┬─────┘                             ▼                                    │
│        │                        ┌──────────────────┐                            │
│        └───────────────────────▶│ ruleengine       │                            │
│                                 │ 规则更新 (越用越聪明) │                            │
│                                 └──────────────────────┘                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 BrainAgent 核心设计

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              BrainAgent 架构图                                       │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                           BrainAgent (大脑核心)                               │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐ │   │
│  │  │                     React 循环分析引擎                                   │ │   │
│  │  │  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐             │ │   │
│  │  │  │ Thought │───▶│ Action  │───▶│ Observe │───▶│ Reason  │─────┐       │ │   │
│  │  │  │ (思考)   │    │ (行动)  │    │ (观察)  │    │ (推理)  │     │       │ │   │
│  │  │  └─────────┘    └─────────┘    └─────────┘    └─────────┘     │       │ │   │
│  │  │       ▲                                                       │       │ │   │
│  │  │       └───────────────────────────────────────────────────────┘       │ │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘ │   │
│  │                                      │                                         │   │
│  │         ┌────────────────────────────┼────────────────────────────┐         │   │
│  │         ▼                            ▼                            ▼         │   │
│  │  ┌──────────────┐          ┌──────────────┐           ┌──────────────┐     │   │
│  │  │AgentRegistry │          │ToolRegistry  │           │ContextManager│     │   │
│  │  │(子Agent管理) │          │(工具能力)     │           │(上下文管理)  │     │   │
│  │  │              │          │              │           │              │     │   │
│  │  │ • 注册/注销   │          │ • 注册/调用  │           │ • 会话历史   │     │   │
│  │  │ • 状态监控   │          │ • 工具描述    │           │ • 变量存储   │     │   │
│  │  │ • 生命周期   │          │ • 可用性检查  │           │ • 状态追踪   │     │   │
│  │  └──────────────┘          └──────────────┘           └──────────────┘     │   │
│  │                                                                              │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐ │   │
│  │  │                     Agent 动态生成与调度                               │ │   │
│  │  │                                                                       │ │   │
│  │  │   用户输入 ──▶ 大脑分析 ──▶ 按需创建子Agent ──▶ 分配任务 ──▶ 监控状态    │ │   │
│  │  │                                                                       │ │   │
│  │  │   ┌────────────────────────────────────────────────────────────┐     │ │   │
│  │  │   │ 预定义 Agent:                                               │     │ │   │
│  │  │   │   • RuleGenerationAgent    (规则生成)                       │     │ │   │
│  │  │   │   • RuleOptimizationAgent  (规则优化)                       │     │ │   │
│  │  │   │   • DataAnalysisAgent      (数据分析)                       │     │ │   │
│  │  │   │   • IntentRecognitionAgent (意图识别)                       │     │ │   │
│  │  │   │   • ActionExecutionAgent   (行动执行)                       │     │ │   │
│  │  │   └────────────────────────────────────────────────────────────┘     │ │   │
│  │  │                                                                       │ │   │
│  │  │   ┌────────────────────────────────────────────────────────────┐     │ │   │
│  │  │   │ 动态 Agent: (根据需求运行时创建)                              │     │ │   │
│  │  │   │   • 自定义分析 Agent                                         │     │ │   │
│  │  │   │   • 临时任务 Agent                                           │     │ │   │
│  │  │   └────────────────────────────────────────────────────────────┘     │ │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘ │   │
│  │                                                                              │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐ │   │
│  │  │                     定期分析 + 动作决策                                 │ │   │
│  │  │   • 定期扫描主流程报告                                                │ │   │
│  │  │   • 分析异常模式                                                      │ │   │
│  │  │   • 决策：生成新规则 / 优化规则 / 发送通知 / 生成报告                  │ │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心接口定义

```java
// StreamOrchestrator - 核心编排器
public interface StreamOrchestrator {
    void start();           // 启动主流程
    void stop();           // 停止
    void submitUserInput(String prompt);  // 用户输入
    void triggerAnalysis();              // 触发分析
}

// UserInputHandler - 用户输入处理
public interface UserInputHandler {
    Intent handleInput(String prompt);   // 处理用户输入
    void submitIntent(Intent intent);    // 提交意图
}

// RuleEngine - 规则引擎(支持匹配结果)
public interface RuleEngine {
    RuleMatchResult evaluate(CdcEvent event, RuleContext context);
    void addRule(Rule rule);
    void removeRule(String ruleId);
}

// ScheduledAnalyzer - 定期LLM分析
public interface ScheduledAnalyzer {
    void analyze(List<CdcEvent> events);  // 分析未拦截数据
    void schedule(AnalysisTask task);     // 调度任务
    void cancel(String taskId);
}

// ReportGenerator - 报告生成(支持配置)
public interface ReportGenerator {
    Report generate(ReportContext context);
    boolean isEnabled();                  // 检查是否生成
}

// NotificationService - 通知服务
public interface NotificationService {
    void send(Notification notification);
    boolean isEnabled();                  // 检查是否发送
}
```

## 3. 包依赖关系

```
api/
  ├── controller/ → service/ → (orchestrator, agent, ruleengine, report)
  │
orchestrator/
  ├── StreamOrchestrator → (datasource, agent, ruleengine, pipeline, report)
  ├── UserInputHandler → agent (大脑)
  └── SchedulerManager → agent (大脑定期分析)
  │
agent/                                # 新增：Agent 管控层
  ├── BrainAgent → (AgentRegistry, ToolRegistry, ReactLoop)
  ├── AgentRegistry → BaseAgent (管理子 Agent)
  ├── ToolRegistry → AgentTool (管理工具)
  ├── ReactLoop → LlmService (推理分析)
  ├── RuleGenerationAgent → LlmService
  ├── RuleOptimizationAgent → LlmService
  └── (子 Agent) → ToolRegistry (调用工具)
  │
datasource/ → (被 ruleengine 引用)
  │
ruleengine/ ← (agent 调用)
  │
pipeline/ ← (llm 分析结果)
  │
llm/
  ├── rulegen/ → ruleengine/
  ├── ruleopt/ → ruleengine/
  ├── scheduled/ → ruleengine/
  └── rulegen/ → report/
  │
report/ → notification/
  │
notification/ → (config 控制开关)
  │
config/ → (所有模块引用)
  │
exception/ → (所有模块引用)
```

## 4. 新增模块开发优先级

| 优先级 | 模块 | 依赖 | 说明 |
|:------:|------|------|------|
| P0 | orchestrator | datasource | 核心控制层，流程编排 |
| P0 | ruleengine | datasource | 规则引擎核心，实时拦截 |
| P0 | pipeline | datasource, ruleengine | 数据流处理，非拦截数据 |
| P0 | agent (BrainAgent) | llm, tool | **大脑核心**，管控子Agent + React循环 + 工具调用 |
| P1 | agent (子Agent) | agent, ruleengine | 子Agent实现：规则生成/优化/分析/执行 |
| P1 | llm (意图理解) | orchestrator, agent | 用户输入理解 |
| P1 | llm.rulegen | llm, ruleengine, agent | 规则生成，核心闭环 |
| P1 | llm.ruleopt | llm, ruleengine, agent | 规则优化，自进化 |
| P1 | llm.scheduled | llm, pipeline, agent | 定期LLM分析，非拦截数据 |
| P2 | report | pipeline, llm | 报告生成，按需 |
| P2 | notification | report | 邮件通知，按需 |
| P2 | api | 全部 | REST API，对外接口 |

---

## 5. BrainAgent 核心接口定义

```java
// BrainAgent - 大脑 Agent 主类
public interface BrainAgent extends BaseAgent {
    // React 循环分析
    ReactResult think(String input, AgentContext context);

    // 决策是否需要调用子 Agent
    Decision decide(String thought);

    // 动态创建子 Agent
    BaseAgent createAgent(AgentSpec spec);

    // 获取所有 Agent 状态
    List<AgentRegistry.AgentSnapshot> getAllAgentStatus();

    // 定期分析报告，决策动作
    ActionResult analyzeAndAct(Report report);

    // 询问用户（React 循环中可随时暂停）
    InputResult askUser(String question);
}

// AgentRegistry - Agent 注册表
public interface AgentRegistry {
    boolean register(BaseAgent agent);
    boolean unregister(String agentId);
    BaseAgent getAgent(String agentId);
    List<BaseAgent> getAgentsByType(String agentType);
    List<AgentRegistry.AgentSnapshot> getAllSnapshots();
    void addEventListener(AgentEventListener listener);
}

// ToolRegistry - 工具注册表
public interface ToolRegistry {
    boolean register(AgentTool tool);
    ToolResult invoke(String toolName, Map<String, Object> params);
    List<ToolDescriptor> getToolDescriptors();
    boolean isToolAvailable(String toolName);
}

// ReactLoop - React 循环分析引擎
public interface ReactLoop {
    ReactResult execute(String input, AgentContext context);
    void setMaxIterations(int maxIterations);
    void setAskUserCallback(AskUserCallback callback);
}
```

*文档版本: v2.1*
*基于架构文档 v2.0 + 大脑 Agent 核心设计*
