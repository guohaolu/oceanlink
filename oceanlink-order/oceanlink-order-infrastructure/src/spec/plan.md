# oceanlink-order-infrastructure 实现方案

## 当前方案

- 先使用内存实现打通依赖注入和最小运行链路
- 真实基础设施能力以后续 GatewayImpl 逐步替换

## 后续方案

- 实现数据库持久化
- 实现 Amazon HTTP 客户端
- 实现审计和 Outbox
