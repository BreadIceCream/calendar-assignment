# 📅 Smart Calendar - 智能日历应用

一款基于 Android 原生技术栈开发的功能完备的日历应用，采用 Jetpack Compose 构建现代化 UI。

## ✨ 功能特性

- 🗓️ **多视图切换** - 月视图、周视图、日视图三种展示方式
- 🌙 **完整农历支持** - 农历日期、节气、传统节日、干支纪年
- 📝 **日程管理** - 创建、编辑、删除日程，支持批量操作
- ⏰ **智能提醒** - 多种提醒选项，支持开机恢复
- 📤 **ICS 导入导出** - 符合 RFC5545 标准，与其他日历互通
- 🌐 **网络订阅** - 订阅在线日历（如节假日日历）

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | 声明式 UI 框架 |
| Room | 本地数据库 |
| MVVM | 架构模式 |
| StateFlow | 状态管理 |
| Navigation Compose | 页面导航 |
| Material 3 | UI 设计规范 |

## 📱 系统要求

- Android 8.0 (API 26) 或更高版本
- Android Studio Ladybug 或更高版本

## 🚀 快速开始

1. 克隆仓库
```bash
git clone https://github.com/BreadIceCream/calendar-assignment.git
```

2. 使用 Android Studio 打开项目

3. 同步 Gradle 依赖

4. 运行到模拟器或真机

## 📁 项目结构

```
app/src/main/java/com/example/calendar/
├── data/           # 数据层 (Entity, DAO, Repository)
├── ui/             # UI层 (Compose 组件)
├── viewmodel/      # ViewModel 层
├── notification/   # 通知提醒系统
├── sync/           # 订阅同步服务
└── util/           # 工具类 (农历、日期、ICS解析)
```

## 📄 文档

- [需求文档](./需求文档.md) - 软件需求规格说明书

## 📝 License

This project is for educational purposes.
