# YashanDB · CDC · FOR · AI

[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.4-blue.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-YashanDB-lightgrey.svg)](LICENSE)

> 让大模型成为企业私有数据的"业务大脑"，让数据库从"存储仓库"进化为"智能中枢"

<p align="center">
  <a href="http://111.231.32.106:443/"><b>在线企划书</b></a> ·
  <a href="#quick-start"><b>快速开始</b></a> ·
  <a href="#architecture"><b>系统架构</b></a> ·
  <a href="#scenarios"><b>应用场景</b></a> ·
  <a href="#contributing"><b>参与贡献</b></a>
</p>

---

## 项目简介

将 CDC（Change Data Capture）实时数据捕获与大语言模型原生融合的智能数据分析系统。基于 YashanDB 自研 YDS CDC 产品，实现端到端秒级实时同步，构建 **"感知 → 思考 → 行动"** 完整闭环。

- **实时**：端到端秒级同步，源端毫秒级捕获
- **智能**：LLM 作为大脑调度器，自然语言生成实时规则
- **安全**：数据不出域，私有部署，支持国产大模型

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| 构建 | Gradle 8.4 |

## Quick Start

### 前置要求

- JDK 17+
- Gradle Wrapper（项目自带）

### 构建

```bash
git clone <repo-url> && cd stream-ai
./gradlew clean build
```

### 运行

```bash
java -jar build/libs/stream-ai-*.jar
```

### 代码格式化

```bash
./gradlew spotlessApply
```

## 架构

### 核心设计

大模型 = **大脑**（调度、生成规则），规则引擎 = **双手**（实时执行）。不是让大模型做实时计算，而是让大模型**指挥**实时计算。

```
┌─────────────────────────────────────────────────┐
│              用户交互层                           │
│         自然语言 / Web / API                      │
├─────────────────────────────────────────────────┤
│        大模型大脑 (LLM Orchestrator)              │
│    理解意图 → 技能选择 → 流程编排 → 监控          │
├─────────────────────────────────────────────────┤
│         技能编排引擎 + Skill 库                   │
│  监控报警 · 异常隔离 · 数据对账 · 分析 · 通信     │
├─────────────────────────────────────────────────┤
│           数据流处理层                            │
│   YDS CDC 实时流 · 端到端秒级 · 源端毫秒级        │
├─────────────────────────────────────────────────┤
│             三层判断机制                          │
│ 规则引擎(ms) + 预处理过滤(ms) + 深度分析(s)       │
└─────────────────────────────────────────────────┘
```

### 三层决策

| 层级 | 名称 | 速度 | 说明 |
|:--:|:--:|:--:|------|
| **1** | `chat2rule` | 毫秒 | 自然语言 → LLM 生成规则 → 规则引擎执行（覆盖 80-90%） |
| **2** | 预处理过滤 | 毫秒 | CDC 数据清洗/压缩/抽取，提取关键信息 |
| **3** | `Unexpected2chat` | 秒级 | 异常 → LLM 深度分析 → 规则反向优化（越用越聪明） |

### CDC 数据捕获

支持 DDL/DML、存储过程、XML/JSON 半结构化、地理空间、向量数据共 **6 大类**变更捕获，全部实时同步。

## 应用场景

<table>
<tr>
<td>

**电商风控**

订单实时风控，多表联动分析，自动发现刷单模式

</td>
<td>

**金融反洗钱**

大额转账 + 拆分交易实时监测，规则自动进化

</td>
<td>

**电信对账**

计费/CRM 实时对账，秒级定位差异根因

</td>
</tr>
<tr>
<td>

**医疗安全**

患者数据变更实时监控，越权操作即时告警

</td>
<td>

**智能制造**

产线传感器监控 + 预测性维护，跨表关联根因分析

</td>
<td></td>
</tr>
</table>

## 项目结构

```
.
├── src/main/java/com/yashandb/streamai/
│   └── StreamAiApplication.java      # 主入口
├── src/main/resources/
│   └── log4j2.xml                    # 日志配置
├── src/test/java/                    # 测试代码
├── build.gradle                      # 构建配置
├── gradle.properties                 # 版本常量
├── lombok.config                     # Lombok 配置
├── .github/
│   ├── workflows/ci-build.yml        # CI 流水线
│   └── ISSUE_TEMPLATE/               # Issue 模板
├── .husky/commit-msg                 # 提交校验 hook
├── .commitlintrc.json                # 提交规则
├── LICENSE                           # 许可协议
└── README.md
```

## 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)，格式：

```
type<#issue号>: 描述
```

```bash
feat<#42>: add GitHub Actions CI
fix<#101>: resolve NPE in log config
docs<#5>: update README
```

详见 [提交规范](#contributing)。

## 参与贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat<#x>: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

> 首次贡献前请执行 `npm install && npm run prepare` 安装 commit hook。

## 许可证

本项目受 [YashanDB 软件许可协议](LICENSE) 保护。商业使用需另行获取授权。

---

> Talk is cheap, **Show Me Your Code** — 实干精神，技术驱动
