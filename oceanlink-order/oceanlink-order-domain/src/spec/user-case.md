# oceanlink-order-domain 用户场景

## 模块目标

`oceanlink-order-domain` 负责承载订单域核心规则，包括履约映射、通知去重、订单合并、PII 访问控制和同步任务领域对象。

## 当前范围

- FulfilledBy 映射
- 通知幂等与路由
- 订单合并
- PII 访问策略

## 关联说明

当前设计继承父模块 [用户场景说明](../../src/spec/user-case.md)。
