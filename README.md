# 项目结构

| 模块名               | 描述    |
|-------------------|-------|
| oceanlink-api     | api模块 |
| oceanlink-common  | 公共模块  |
| oceanlink-bom     | 版本控制  |
| oceanlink-market  | 市场模块  |
| oceanlink-product | 商品模块  |

# 待开发项

## 基本功能
1. mybatis-plus中关于clickhouse的[sql注入器](https://baomidou.com/guides/sql-injector/)，实现诸如：异步插入等方法
2. 通过[moneta](https://github.com/JavaMoney/jsr354-ri)实现统一的货币架构
3. 通过EasyExcel实现：多Excel流式监听器；通过模版导出单sheet多表Excel；构建通用的导入组件
4. 实现对飞书的集成，通过webhook实现预警功能
5. 基于拓展点机制扩展canal对接的目标数据源。

## 中间件的使用
1. 通过Caffeine，Zookeeper，Redis实现多级缓存架构设计
2. 基于MySQL，[canal](https://github.com/alibaba/canal/wiki/QuickStart)，Clickhouse实现数据异构，支持多维度查询
3. 基于ProxySQL实现MySQL的读写分离，流量镜像，故障转移

## 性能分析与优化
1. 集成[eclipse collection](https://github.com/eclipse-collections/eclipse-collections)类库，实现数据的压缩和高效运算，需要监控内存使用情况
2. 通过[skywalking](https://skywalking.apache.org/)实现分布式链路的性能监控，使用[clickhouse](https://clickhouse.com/docs)存储
3. 内存分析
4. 线程分析

# 系统设计

## 货币商城体系
参考vivo技术团队对JSR354的[技术分享](https://zhuanlan.zhihu.com/p/445045886)，支持跨境电商下的不同货币之间的转换，拓展了在线商城的虚拟货币类型，如：积分，钻石，鼓励金。