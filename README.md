# 🚀 刷题神器 (Questions Magic Tool)
一个基于 Spring Boot + Redis + Elasticsearch 构建的高性能、高可用的企业级面试刷题与知识管理平台。
## 📖 项目简介
本项目是一个面向广大开发者与求职者的在线刷题平台。除了提供核心的题目与题库管理体系外，项目重点着眼于**高并发场景下的性能优化**与**微服务架构下的高可用治理**。通过引入 Elasticsearch、Sentinel、Nacos 等中间件，实现了毫秒级全文检索、动态防护与细粒度的流量控制。

## 🛠️ 核心技术栈

- **后端框架**: Java 17, Spring Boot 2.7.x, MyBatis-Plus
- **数据存储**: MySQL 8.0
- **缓存与分布式**: Redis, Redisson
- **搜索引擎**: Elasticsearch 7.x, Kibana, IK 分词器
- **服务治理与配置**: Alibaba Sentinel, Nacos
- **工具库**: Hutool (BitMapBloomFilter)

---

## ✨ 核心特性与技术亮点

### 1. 基础业务基石 (RBAC & CRUD)

- 完善的用户体系与管理员权限控制。
- 提供题目（Question）、题库（QuestionBank）以及两者映射关系的增删改查全套 API。

### 2. 🚀 高性能“年度刷题记录” (基于 Redisson 位图)
摒弃了传统的数据库全表扫描统计方式，采用 **Redis Bitmap (位图)** 记录用户全年的活跃状态。

- **内存级加载**: 引入 Redisson 客户端，将用户的签到数据作为 `RBitSet` 直接加载到应用内存中，极大减少了与 Redis 之间的网络 I/O 开销。
- **极速统计算法**: 放弃低效的按天 `for` 循环遍历，底层利用 `nextSetBit(int fromIndex)` 方法直接跳过连续的 `0` 位（未打卡天数），时间复杂度极低，实现千万级用户年度报告的毫秒级生成。

### 3. 🔍 海量题库毫秒级检索 (Elasticsearch + IK 分词)
针对复杂题目检索场景，引入 Elasticsearch 替代 MySQL 的 `LIKE` 模糊查询。

- **精准分词**: 结合 IK 中文分词器，在索引构建时使用 `ik_max_word` 保证高召回率，在用户搜索时使用 `ik_smart` 保证高准确率。
- **双引擎数据同步**: 设计并实现了 MySQL 到 ES 的双重同步机制：
  - **全量同步**: 用于系统初始化或灾备重建。
  - **增量同步**: 基于定时任务扫描数据变更时间，确保双写一致性。
- **可视化**: 接入 Kibana 进行搜索数据的分析与大屏可视化。

### 4. 🛡️ 高可用与细粒度流量控制 (Alibaba Sentinel)
面对恶意刷量和突发流量，接入 Sentinel 实现了立体化的系统保护机制：

- **注解式资源定义**: 针对 `listQuestionBankVOByPage`（题库分页列表）接口，基于 `@SentinelResource` 注解并结合控制台规则，实现快速的限流与降级配置。
- **编程式精准防刷**: 针对 `listQuestionVOByPage`（题目分页列表）接口，创新性地实现了**基于单 IP 的限流熔断**。为了避免注解模式下默认将所有方法参数注入 `SphU.entry(res, args)` 造成的统计维度错乱与内存浪费，主动采用**编程式定义资源**，精准控制限流粒度，极大提升了流控的准确性。

### 5. 🛑 动态 IP 黑名单与布隆过滤安全网 (Nacos + Hutool)
构建了一套无需重启服务的动态恶意 IP 防护屏障：

- **动态配置下发**: 使用 **Nacos** 作为配置中心，集中存储和管理 IP 黑名单列表。修改黑名单秒级生效，零停机。
- **高效过滤器拦截**: 在后端入口处统一部署 Web Filter，对所有请求的真实 IP 进行侦测。
- **布隆过滤器鉴权**: 引入 Hutool 的 `BitMapBloomFilter`，在内存中对请求 IP 与黑名单进行极速匹配。相比于 `HashSet` 或数据库查询，布隆过滤器在面对百万级黑名单时，依然能保持极低的内存占用和 $O(1)$ 的判断速度。

### 6. ⚡ 海量数据批处理优化
针对管理员的高频海量操作（如：批量向题库增删题目、批量物理删除题目），重构了底层批处理逻辑，优化了 JDBC 批处理参数与事务边界，防止大事务造成的数据库锁表与内存溢出（OOM），显著提升了数据吞吐量。

---

## 📂 项目结构

```plaintext
questions-magic-tool/
├── src/main/java/com/sht/stsq/
│   ├── blackfilter/    # 动态 IP 黑名单与布隆过滤器实现
│   ├── config/         # Redisson, ES, MyBatis-Plus 等核心配置
│   ├── controller/     # 接口路由
│   ├── esdao/          # Elasticsearch 数据访问层
│   ├── mapper/         # MyBatis 数据访问层
│   ├── model/          # 实体类、DTO、VO 数据模型
│   ├── service/        # 业务逻辑层 (含 Sentinel 编程式流控逻辑)
│   └── job/            # ES 增量与全量同步定时任务
└── src/main/resources/
    ├── application.yml # 核心配置文件
    └── mapper/         # MyBatis XML 映射文件

```

## 🚀 快速开始

### 环境依赖
请确保你的本地或服务器已安装以下环境：

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Elasticsearch 7.17.x & Kibana 7.17.x
- Nacos 2.x
- Sentinel Dashboard 1.8.x

### 启动步骤

1. 克隆项目到本地。
2. 在 MySQL 中执行 `sql/schema.sql` 完成数据库初始化。
3. 修改 `application.yml` 中的数据源、Redis、ES 以及 Nacos 地址。
4. 启动 Nacos，并增加对应的 `blackIpList` 配置。
5. 运行 `MainApplication.java` 启动 Spring Boot 服务。

---
*Developed with ❤️ by 一个专业的全栈开发工程师.*

---

*Exported from [Voyager](https://github.com/Nagi-ovo/gemini-voyager)*  
*Generated on April 19, 2026 at 04:26 PM*