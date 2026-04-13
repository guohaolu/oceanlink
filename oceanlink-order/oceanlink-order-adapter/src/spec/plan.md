# oceanlink-order-adapter 实现方案

## 当前方案

- 采用轻量 REST + Adapter 入口模式
- 所有业务编排委托给 `app` 模块

## 后续方案

- 增加真实 SQS payload 解析
- 增加 Assembler 和查询接口
