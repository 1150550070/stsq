<div align="center">
  <h1>🚀 刷题神器 (Questions Magic Tool)</h1>
  <p>一个现代化的前后端分离全栈应用项目，致力于提供高效、便捷的学习与刷题体验。</p>

  <!-- 你可以在这里添加项目徽章 (Badges) -->
  <p>
    <img src="https://img.shields.io/badge/Frontend-React%2018-blue" alt="React" />
    <img src="https://img.shields.io/badge/Framework-Next.js-black" alt="Next.js" />
    <img src="https://img.shields.io/badge/UI-Ant%20Design-1890ff" alt="Antd" />
    <img src="https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Language-Java-orange" alt="Java" />
  </p>
</div>

## 📖 项目简介

**刷题神器**平台目前正处于开发阶段，已经完成了核心架构的初始化工作。项目采用当前主流的前后端分离架构，旨在打造一个高性能、易用性强且极具现代交互感的 Web 应用。

---

## 🛠️ 技术栈说明

### 💻 前端 (Frontend)
前端项目位于 `questions-magic-tool-frontend` 目录下，采用企业级的高性能框架：

- **核心框架**: [React 18](https://react.dev/) - 构建用户界面的 JavaScript 库，支持最新的并发特性。
- **元框架**: [Next.js](https://nextjs.org/) - 强大的 React 框架，提供极佳的 SSR (服务端渲染) 和丰富的路由机制。
- **UI 组件库**: [Ant Design (antd)](https://ant.design/index-cn) - 企业级 UI 设计语言和 React 组件库。
- **高级组件**: [ProComponents](https://procomponents.ant.design/) - 基于 Ant Design 的中后台重量级组件库，大幅提升页面开发效率。

### ⚙️ 后端 (Backend)
后端为标准的 Java 企业级应用，提供稳定可靠的 API 架构：

- **核心框架**: Spring Boot体系
- **开发语言**: Java
- **其他技术**: （如 MyBatis/MyBatis-Plus, MySQL, Redis 等，将随着业务需求逐步集成）

---

## 🚀 快速上手 (Getting Started)

### 环境依赖
在运行项目前，请确保您的本地环境中已安装以下软件：
- **Node.js** (推荐 v18 或更高版本)
- **npm** / **yarn** / **pnpm**
- **JDK** (建议 JDK 8 或 17+)
- **Maven**
- **Git**

### 1. 前端项目启动
```bash
# 1. 进入前端工程目录
cd questions-magic-tool-frontend

# 2. 安装项目依赖 (如遇依赖冲突，可添加 --legacy-peer-deps 参考)
npm install

# 3. 启动本地开发服务器
npm run dev
```
> 第一步启动后，在浏览器访问 `http://localhost:3000` 即可预览前端页面。

### 2. 后端项目启动
- 使用您偏好的 IDE (IntelliJ IDEA 或 Eclipse) 打开项目根目录。
- 等待 Maven 自动下载依赖。
- 修改 `src/main/resources/` 目录下的相关配置文件（如数据库连接等信息）。
- 运行 `MainApplication.java` 即可启动服务端。

---

## 📁 项目目录结构

待补充详细的目录结构说明。目前的宏观结构如下：

```text
questions-magic-tool/
├── questions-magic-tool-frontend/  # 前端代码目录 (React + Next.js)
├── src/                            # 后端 Java 源码目录
│   ├── main/
│   │   ├── java/                   # 后端业务逻辑代码
│   │   └── resources/              # 后端配置文件目录 (如 application.yml)
│   └── test/                       # 单元测试目录
├── pom.xml                         # Maven 依赖配置文件
└── README.md                       # 项目说明文档
```


<div align="center">
  <p>Made with ❤️ by the questions-magic-tool team.</p>
</div>
