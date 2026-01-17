## 1. 项目简介

本项目面向以本地可执行程序形式运行的图形应用程序（如 OpenGL 程序），构建一个集 **配置、执行、采集、分析与可视化** 于一体的性能测试系统。

系统以系统级性能测试为基础，并通过 OpenGL 注入扩展支持渲染级性能分析，同时引入 LLM 作为辅助能力，用于测试脚本与性能基准代码的生成与修复。

---

## 2. 核心功能

- 系统级性能指标采集（CPU / 内存 / GPU / FPS）
- 场景化测试配置与动作脚本驱动
- Run 生命周期管理与状态机控制
- 多 Run 统计分析与对比
- OpenGL API 注入与渲染级事件采集
- LLM 辅助测试生成（脚本 / benchmark 草稿）

---

## 3. 仓库结构

```text
graphics-perf-test-system/
├── backend/                    # 后端控制平面（Java / Spring Boot）
│   ├── api-service/            # REST API、Run Orchestrator、Analyzer、LLM Service
│   └── collector-bridge/       # 与 Python Collector / 外部采集模块的通信桥接
├── collector/                  # 执行/采集平面
│   └── python-collector/       # Python 指标采集实现
├── opengl-injection/           # OpenGL API 注入扩展模块
│   └── README.md
├── frontend/                   # 表现平面（Web UI）
│   └── web-ui/
├── docs/                       # 系统文档
│   ├── system-architecture.md
│   ├── system-implementation.md
│   └── design-notes/
├── scripts/                    # 本地开发辅助脚本
├── .gitignore
└── README.md