# oceanlink-order

## 功能说明

`oceanlink-order` 用于承接 OceanLink 的订单域能力。当前阶段先完成 Amazon Seller Central（SC）接单模块设计，目标是把 Amazon SP-API 的订单数据接入、标准化、幂等入库，并向库存、履约、财务等下游输出统一订单事件。

## 模块说明

当前设计范围：

- Amazon Orders API `v2026-01-01`：`searchOrders`、`getOrder`
- Amazon Notifications API：`ORDER_CHANGE`（使用 Amazon SQS 标准队列承接）
- 订单标准化：订单头、订单行、买家信息、配送信息、金额/促销/费用、履约信息
- 接单模式：轮询拉单 + SQS 事件通知协同
- 数据治理：幂等、断点续拉、补单修复、PII 权限隔离

首期强制约束：

- `ORDER_CHANGE + SQS` 必须接通后才算形成可交付闭环。
- Orders API 轮询仅作为通知缺失、延迟或修复场景下的兜底补偿链路，不能替代通知主链路。

当前已确认策略：

- 首期按任意 `marketplaceId` 设计，卖家授权、接单水位、统计口径按 Seller + Marketplace 维度隔离。
- 买家姓名、电话、地址允许明文落库，但仅限受控授权链路，且必须具备访问审计能力。
- `Amazon Business` 等 program 信息首期保留为扩展字段，不单独提升为固定领域字段。

后续代码结构：

- 参考 COLA 5.0，采用 `client / app / domain / infrastructure / adapter / start` 分层。
- `oceanlink-order` 设计为聚合父模块，内部再拆分 COLA 子模块，而不是单一 jar 内部随意分包。
- 计划子模块包括：`oceanlink-order-client`、`oceanlink-order-domain`、`oceanlink-order-app`、`oceanlink-order-infrastructure`、`oceanlink-order-adapter`、`oceanlink-order-start`。

当前实现进度：

- 已完成 COLA 5.0 六子模块骨架和 Maven 聚合接入。
- 已完成第一批 TDD 代码：`FulfilledBy -> FBA/FBM` 映射、通知去重键、订单合并策略、PII 访问策略、通知入任务应用服务。
- 已提供最小可运行骨架：内存版通知/任务网关、SQS 通知适配器、联调控制器、Spring Boot 启动模块。
- Amazon Orders API / Notifications API 的真实 HTTP 出站适配、持久化 DDL、Outbox 与调度任务仍在后续实现范围。

子模块说明：

| 子模块 | 说明 |
|--------|------|
| `oceanlink-order-client` | 命令、DTO、事件和枚举契约 |
| `oceanlink-order-domain` | 订单聚合、同步任务、领域规则与网关接口 |
| `oceanlink-order-app` | 命令执行器和用例编排 |
| `oceanlink-order-infrastructure` | 网关实现、持久化与出站适配 |
| `oceanlink-order-adapter` | REST、SQS、调度等入站适配 |
| `oceanlink-order-start` | Spring Boot 启动与配置装配 |

当前不在本阶段范围：

- 发货确认、取消确认、退货退款
- Amazon 多渠道订单（MCF）履约出库
- 非 Amazon 渠道订单接入

## Amazon 特殊术语

- `SC (Seller Central)`：亚马逊卖家后台。本模块面向 Seller 侧接单，不覆盖 Vendor Central。
- `SP-API (Selling Partner API)`：亚马逊卖家开放接口体系，`Orders API` 和 `Notifications API` 都属于该体系。
- `Orders API`：订单查询接口。当前以 `v2026-01-01` 为设计基线，核心操作是 `searchOrders` 和 `getOrder`。
- `ORDER_CHANGE`：订单变更通知。官方建议通过通知减少对 Orders API 的反复轮询，本模块将其作为增量接单触发器，并通过 Amazon SQS 标准队列接收消息。
- `Destination`：Notifications API 的投递目标。当前模块要求创建 SQS 类型的 destination，并用其 `destinationId` 订阅 `ORDER_CHANGE`。
- `ProcessingDirective`：通知订阅时的处理指令。对 `ORDER_CHANGE` 可通过 `eventFilter` 过滤 `OrderStatusChange`、`BuyerRequestedChange` 等订单变更类型，但不支持 `marketplaceIds` 过滤。
- `MarketplaceId`：站点标识，例如美国站、日本站。接单水位、授权范围、统计口径都需要按站点隔离。
- `IncludedData`：`searchOrders` / `getOrder` 的数据选择参数，可控制是否返回 `buyer`、`recipient`、`packages`、`promotion`、`proceeds` 等块数据。
- `PII (Personally Identifiable Information)`：买家姓名、地址、邮箱、电话等敏感数据。设计上必须按授权链路获取，允许明文落库，但要做权限隔离、访问审计和最小暴露。
- `Restricted role`：访问订单敏感数据所需的受限角色。在 Orders API `v2026-01-01` 中，符合条件时可直接通过 `includedData` 获取订单敏感数据，不再通过 Orders API 额外申请 `RDT`。
- `FulfilledBy`：订单履约主体。`AMAZON` 可按业务语义映射为 `FBA`，`MERCHANT` 可按业务语义映射为 `FBM`；模块设计中需同时保留 Amazon 原始枚举值，避免丢失更细分的商家履约语义。
- `Amazon programs`：订单或订单行可能挂载的项目标签，如 `PRIME`、`AMAZON_BUSINESS`、`TRANSPARENCY`、`SUBSCRIBE_AND_SAVE`，需要保留为扩展字段。
- `多渠道订单 (Multi-Channel Fulfillment, MCF)`：使用亚马逊履约网络配送非 Amazon 渠道订单的模式。该能力归属 `Fulfillment Outbound API`，不是当前 Amazon SC 接单模块的首期范围。
- `Backfill`：首次接入或故障修复时按时间窗口回补历史订单。
- `Watermark`：增量接单时维护的时间游标，通常围绕 `lastUpdatedAfter` / `lastUpdatedBefore` 设计。
- `SQS Standard Queue`：Amazon 官方通知接入队列类型。SP-API 不支持 FIFO 队列，因此消费侧必须接受重复消息和近似有序而非严格有序。

## 启动方式

当前模块已完成设计和首批基础实现，完整 Amazon 接单链路仍在持续补齐。当前可评审文档：

- [用户场景说明](src/spec/user-case.md)
- [开发任务说明](src/spec/task.md)
- [实现方案说明](src/spec/plan.md)

当前已落地的启动模块为 `oceanlink-order-start`，后续可直接作为订单域服务启动入口。

当前本地验证前提：

```powershell
mvn -f oceanlink-bom/pom.xml install
```

原因是当前根工程依赖本地可解析的 `oceanlink-bom`，首次构建前需要先把 BOM 安装到本地 Maven 仓库。

## 配置说明

当前尚未落地运行时配置。设计阶段已确认后续需要至少覆盖以下配置项：

- Amazon 应用凭证与刷新令牌
- Seller 账号与 `marketplaceIds`
- 轮询窗口、回补窗口、补偿任务开关
- Amazon SQS 队列 ARN、`destinationId`、订阅配置
- SQS 队列策略与可选 KMS 权限配置
- PII 明文落库权限、访问审计与脱敏展示开关

当前 `oceanlink-order-start` 已提供的最小配置项：

- `order.amazon.sp-api.endpoint`
- `order.amazon.sp-api.connect-timeout-seconds`
- `order.amazon.sp-api.read-timeout-seconds`
- `order.amazon.sp-api.default-included-data`
- `order.amazon.sqs.queue-arn`
- `order.amazon.sqs.queue-url`
- `order.amazon.sqs.max-messages`
- `order.amazon.sync.scheduler.*`

## 测试说明

当前已经落地并通过的测试包括：

- `OrderFulfillmentModeMapperTest`
- `OrderNotificationIdentityFactoryTest`
- `OrderMergePolicyTest`
- `OrderPiiAccessPolicyTest`
- `OrderNotificationCmdExeTest`

验证命令：

```powershell
mvn -f oceanlink-bom/pom.xml install
mvn -pl oceanlink-order/oceanlink-order-start -am test
```

后续仍需继续补充：

- Orders API 客户端契约测试
- 拉单游标与翻页测试
- 幂等入库测试
- 通知乱序/重复测试
- SQS 消息消费与可见性超时测试
- PII 明文落库与权限隔离测试

## 关键变更记录

- 2026-04-13：初始化 `oceanlink-order` 模块文档，确定首期范围为 Amazon SC 接单模块，并补充 Amazon Orders API 相关术语表。
- 2026-04-13：补充 `ORDER_CHANGE` 通知接入约束，明确使用 Amazon SQS 标准队列作为通知承载通道。
- 2026-04-13：确认首期必须接通 `ORDER_CHANGE + SQS`，不接受仅靠轮询构成最小闭环。
- 2026-04-13：确认首期按任意 `marketplaceId` 设计，允许买家关键信息明文落库，`Amazon Business` 等 program 先保留为扩展字段。
- 2026-04-13：补充 `ORDER_CHANGE` 订阅粒度约束，明确 `processingDirective` 不支持 `marketplaceIds` 过滤，并完成 `plan.md` 设计方案。
- 2026-04-13：根据 COLA 5.0 重构模块设计，`oceanlink-order` 改为聚合父模块，内部采用 `client/app/domain/infrastructure/adapter/start` 子模块划分。
- 2026-04-13：完成 COLA 多模块骨架和首批 TDD 代码，订单域已具备最小可编译、可测试、可启动的基础实现。

## 参考资料

- Selling Partner API Models 仓库：https://github.com/amzn/selling-partner-api-models
- Orders API：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/orders-api
- Get orders with filtering criteria：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/get-orders-with-filtering-criteria
- Orders API model (`2026-01-01`)：https://github.com/amzn/selling-partner-api-models/blob/main/models/orders-api-model/orders_2026-01-01.json
- Notifications API model：https://github.com/amzn/selling-partner-api-models/blob/main/models/notifications-api-model/notifications.json
- Java auth/client templates：https://github.com/amzn/selling-partner-api-models/tree/main/clients/sellingpartner-api-aa-java
- Subscribe to the `ORDER_CHANGE` notification：https://developer-docs.amazon.com/sp-api/docs/tutorial-subscribe-to-order-change-notification
- Set up notifications using Amazon SQS：https://developer-docs.amazon.com/sp-api/lang-ja_JP/docs/set-up-notifications-with-amazon-sqs
- Grant the SP-API permission to an Amazon SQS queue：https://developer-docs.amazon.com/sp-api/lang-US/docs/tutorial-grant-permission-to-sqs-queue
- Access Orders PII：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/access-orders-pii
- Orders API rate limits：https://developer-docs.amazon.com/sp-api/docs/orders-api-rate-limits
- Fulfillment Outbound API reference：https://developer-docs.amazon.com/sp-api/docs/fulfillment-outbound-api-v2020-07-01-reference
