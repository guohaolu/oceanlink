# OceanLink

基于 Spring Cloud 的电商/商城技术中台项目，聚焦多级缓存、数据异构、货币体系与可观测性等能力建设。

## 技术栈

| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 2.7.6、Spring Cloud 2021.0.8、Spring Cloud Alibaba 2021.0.5.0 |
| 服务治理 | Nacos（注册中心/配置中心）、OpenFeign、Sentinel、Seata |
| 网关 | Spring Cloud Gateway |
| 数据访问 | MyBatis-Plus、MySQL、动态数据源、ClickHouse |
| 缓存 | Caffeine、Redis、Redisson |
| 消息与流 | Spring Cloud Stream (Kafka) |
| 可观测 | SkyWalking、Spring Boot Admin、Logstash Logback Encoder |
| 工具库 | Hutool、Guava、MapStruct、Lombok、EasyExcel、JSR 354 (Moneta) |

- **JDK**：17  
- **构建工具**：Maven 3.x  

## 环境要求

- JDK 17+
- Maven 3.6+
- 可选：Nacos、MySQL、Redis、Kafka、ClickHouse 等（按需启动对应模块）

## 快速开始

```bash
# 克隆项目
git clone <repository-url>
cd oceanlink

# 编译（跳过测试）
mvn clean install -DskipTests

# 编译并执行测试
mvn clean install
```

各业务服务与网关需在配置好 Nacos、数据库等后单独启动。

## 项目结构

| 模块名 | 描述 |
|--------|------|
| **oceanlink-bom** | 统一依赖版本管理（Bill of Materials） |
| **oceanlink-common** | 公共模块（工具、货币、缓存等基础能力） |
| **oceanlink-api** | 开放 API 模块（DTO、接口定义等） |
| **oceanlink-gateway** | 网关模块 |
| **oceanlink-product** | 商品模块 |
| **oceanlink-market** | 市场模块 |
| **oceanlink-cache-pipeline** | 缓存管道模块（多级缓存一致性） |
| **oceanlink-benchmark** | 基准测试模块（JMH） |

## 待开发项

### 基本功能

1. MyBatis-Plus 针对 ClickHouse 的 [SQL 注入器](https://baomidou.com/guides/sql-injector/)，实现异步插入等方法  
2. 基于 [Moneta](https://github.com/JavaMoney/jsr354-ri) 的统一货币架构（已在 common 中引入 JSR 354，待完善业务封装）  
3. 基于 EasyExcel：多 Excel 流式监听器；模版导出单 Sheet 多表 Excel；通用导入组件  
4. 飞书集成：通过 Webhook 实现预警能力  
5. 基于扩展点机制扩展 Canal 对接的目标数据源  

### 中间件使用

1. 基于 Caffeine、Zookeeper、Redis 的多级缓存架构  
2. 基于 MySQL + [Canal](https://github.com/alibaba/canal/wiki/QuickStart) + ClickHouse 的数据异构，支持多维度查询  
3. 基于 ProxySQL 的 MySQL 读写分离、流量镜像与故障转移  

### 性能分析与优化

1. 集成 [Eclipse Collections](https://github.com/eclipse-collections/eclipse-collections)，做数据压缩与高效运算，并监控内存  
2. 使用 [SkyWalking](https://skywalking.apache.org/) 做分布式链路与性能监控，使用 [ClickHouse](https://clickhouse.com/docs) 存储  
3. 内存分析与线程分析  

## 系统设计

### 货币商城体系

参考 vivo 技术团队对 JSR 354 的 [技术分享](https://zhuanlan.zhihu.com/p/445045886)，支持跨境电商场景下的多币种转换，并扩展在线商城的虚拟货币类型（如积分、钻石、鼓励金等）。

### Caffeine 缓存使用

本地缓存能力与示例位于 **oceanlink-common** 模块下的 Caffeine 相关目录。

### 多级缓存一致性方案

参考 [社交直播多级缓存一致性解决方案-缓存管道-技术分享](https://zhuanlan.zhihu.com/p/656198463)。

- **基于 Binlog 同步**：通过 Canal 等同步 MySQL Binlog，驱动缓存更新  
- **基于注册中心响应**：通过 Nacos 等注册中心下发变更，保证多级缓存一致  

### 跨线程 ThreadLocal 设计

- 阿里开源：[TransmittableThreadLocal](https://github.com/alibaba/transmittable-thread-local)  
- 自定义实现：TransmissibleThreadLocal（项目内扩展）  

---

如有问题或建议，欢迎提 Issue 或参与贡献。
