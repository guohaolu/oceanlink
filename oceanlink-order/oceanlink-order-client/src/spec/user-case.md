# oceanlink-order-client 用户场景

## 模块目标

`oceanlink-order-client` 负责沉淀订单域对内暴露的命令、DTO、事件和枚举契约，避免 `adapter`、`app`、`domain` 之间直接耦合实现细节。

## 当前范围

- Amazon SC 接单的通知命令契约
- 同步任务返回 DTO
- 订单变更枚举

## 关联说明

当前设计继承父模块 [用户场景说明](../../src/spec/user-case.md)。
