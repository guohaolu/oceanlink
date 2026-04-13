# Amazon SC 接单模块实现方案

## 文档目标

本文档基于已确认的 `user-case.md` 和 `task.md`，给出 `oceanlink-order` 首期 Amazon Seller Central（SC）接单模块的落地方案，覆盖：

- DDL 设计
- 类设计
- 模块设计
- 接口设计
- 单元测试设计
- 集成测试设计

本文档是进入正式编码前的最后一层设计约束，后续实现需按照 TDD 顺序推进。

## 官方约束与关键设计决策

### 官方约束

- Orders API 首期采用 `v2026-01-01`，核心操作是 `searchOrders` 和 `getOrder`。
- `searchOrders` 默认限流很低，官方默认速率为 `0.0056 rps`，突发 `20`；`getOrder` 默认限流为 `0.5 rps`，突发 `30`。
- `searchOrders` 的 `paginationToken` 最长有效期为 `24` 小时。
- Orders API 支持最近两年的订单；`JP`、`AU`、`SG` 站点支持 `2016` 年以来的订单。
- `ORDER_CHANGE` 通知必须通过 Amazon SQS Standard Queue 接收，不支持 FIFO 队列。
- `ORDER_CHANGE` 的 `processingDirective` 支持 `orderChangeTypes`，但不支持 `marketplaceIds` 过滤。
- SQS 通知可能重复、乱序，且至少一次投递；官方建议使用 `NotificationMetadata.NotificationId` 做重复判断。

### 设计决策

- 通知主链路采用 `ORDER_CHANGE -> SQS -> getOrder`，因为通知已给出 `AmazonOrderId`，而 `getOrder` 的吞吐能力明显高于 `searchOrders`。
- `searchOrders` 只承担三类职责：首次 backfill、定时 reconciliation、指定时间窗 repair。
- 站点隔离以 `SellerId + MarketplaceId` 为核心维度，但 `ORDER_CHANGE` 订阅本身按 Seller 级管理，站点路由在消费消息时完成。
- PII 允许明文落库，但必须与普通订单字段分表存储，并通过独立应用服务暴露，减少误读风险。
- `Amazon Business`、`Prime`、`Subscribe and Save` 等 program 信息首期保留为扩展字段和原始快照，不提升为固定列。
- 下游事件采用 `Outbox + Spring Cloud Stream(Kafka)` 模式，避免订单写库与消息发送不一致。

### `selling-partner-api-models` 仓库参考策略

参考仓库：

- 根仓库：`amzn/selling-partner-api-models`
- Orders 模型：`models/orders-api-model/orders_2026-01-01.json`
- Notifications 模型：`models/notifications-api-model/notifications.json`
- Java 认证/代码生成模板：`clients/sellingpartner-api-aa-java`

使用原则：

- `models/orders-api-model/orders_2026-01-01.json` 作为 Orders 接口参数、枚举、分页、`includedData` 和响应结构的首要契约源。
- `models/notifications-api-model/notifications.json` 作为 `createDestination`、`createSubscription`、`DestinationResource`、`SqsResource`、`OrderChangeTypeEnum` 的首要契约源。
- `clients/sellingpartner-api-aa-java` 只参考其认证抽象和命名方式，例如 `LWAAuthorizationCredentials`、LWA scope 处理、swagger-codegen 模板扩展点；首期不直接引入其生成客户端。
- 原因是本项目需要与 Spring Boot、MyBatis-Plus、SQS 消费线程模型、Outbox、审计和自定义重试机制深度集成，直接使用生成客户端会增加二次封装成本。
- 编码时优先保证内部 DTO 与 Amazon 模型字段名、枚举值、时间格式保持一致，避免手写结构偏离官方模型。
- 集成测试应尽量使用模型仓库中的定义和官方示例构造 contract fixture，而不是自定义随意 payload。

## 模块总览

### COLA 5.0 模块结构

`oceanlink-order` 不再设计为“单 jar 内部自由分包”，而是参考 COLA 5.0 `cola-archetype-web` 的思路，拆成一个父模块和六个职责明确的子模块：

- `oceanlink-order`
  说明：父聚合模块，`packaging=pom`，负责统一版本、聚合构建、保存模块级 README 和 `src/spec`。
- `oceanlink-order-client`
  说明：对内暴露的 DTO、Command、Query、Response、Event 定义，不承载业务实现。
- `oceanlink-order-domain`
  说明：订单聚合、值对象、领域服务、领域规则、Gateway/Repository 接口。
- `oceanlink-order-app`
  说明：用例编排层，负责 CommandExecutor、QueryExecutor、事务边界、任务编排。
- `oceanlink-order-infrastructure`
  说明：MyBatis-Plus 持久化、Amazon API 出站适配、Outbox 落库与投递实现。
- `oceanlink-order-adapter`
  说明：入站适配层，负责 REST 管理接口、SQS 消费适配、调度任务入口、Assembler。
- `oceanlink-order-start`
  说明：最终启动模块，负责 Spring Boot 装配、配置加载、组件扫描、运行时集成。

依赖方向：

- `client` 不依赖其他 `order` 子模块。
- `domain` 不依赖 `adapter`、`infrastructure`、`start`。
- `app` 依赖 `domain` 和 `client`。
- `adapter` 依赖 `app` 和 `client`。
- `infrastructure` 依赖 `domain`，实现其 Gateway/Repository 接口。
- `start` 依赖 `adapter`、`app`、`infrastructure`，负责最终装配。

包设计原则：

- 先按 COLA 模块切分，再在模块内部按订单域能力分包。
- 入站行为放在 `adapter`，出站依赖放在 `infrastructure`，避免混淆。
- Amazon API 调用属于出站依赖，因此不放在 `adapter`。
- SQS 消费属于入站事件驱动，因此放在 `adapter`。

### 主流程

#### 流程 1：Seller 接入初始化

1. 保存 Seller 级授权配置。
2. 保存 Seller + Marketplace 绑定关系。
3. 校验 SQS ARN、队列策略、可选 KMS 权限。
4. 调用 `createDestination` 创建 SQS destination。
5. 调用 `createSubscription(notificationType=ORDER_CHANGE)` 创建 Seller 级订阅。
6. 初始化各 Marketplace 的 backfill 任务和增量 cursor。

#### 流程 2：通知驱动接单

1. SQS 拉取消息。
2. 基于 `NotificationMetadata.NotificationId` 去重。
3. 解析 `SellerId`、`MarketplaceId`、`AmazonOrderId`、`OrderChangeType`、`EventTime`。
4. 生成 `NOTIFICATION_DRIVEN` 详情补全任务。
5. 调用 `getOrder(orderId, includedData=...)` 获取订单详情。
6. 归一化订单数据并幂等入库。
7. 写入 outbox 事件。
8. 成功后删除 SQS 消息。

#### 流程 3：轮询回补与修复

1. 调度器读取 `PENDING/RUNNING` 的同步任务。
2. 以 Seller + Marketplace 维度调用 `searchOrders`。
3. 维护 `paginationToken`、窗口起止、任务进度。
4. 对每页订单做基础归一化入库。
5. 对字段不完整订单投递 `DETAIL_ENRICHMENT` 任务。
6. 任务完成后推进 cursor。

#### 流程 4：下游事件投递

1. 订单主事务提交时写入 outbox。
2. relay 任务读取待投递事件。
3. 发布至 Kafka / Stream。
4. 记录投递结果并重试失败事件。

## DDL 设计

### 表 1：`order_amazon_seller_account`

用途：

- 存储 Seller 级 Amazon 接入配置和授权状态。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `tenant_id` | varchar(64) | 租户标识 |
| `seller_id` | varchar(64) | Amazon SellerId |
| `region_code` | varchar(16) | `NA/EU/FE` 等区域 |
| `refresh_token_ciphertext` | varchar(2048) | 刷新令牌密文 |
| `lwa_client_id` | varchar(128) | LWA clientId |
| `lwa_client_secret_ciphertext` | varchar(2048) | LWA clientSecret 密文 |
| `status` | varchar(32) | `ENABLED/DISABLED/AUTH_FAILED` |
| `last_auth_check_time` | datetime | 最近鉴权校验时间 |
| `remark` | varchar(512) | 备注 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_tenant_seller(tenant_id, seller_id)`
- 普通索引：`idx_status(status)`

### 表 2：`order_amazon_marketplace_binding`

用途：

- 存储 Seller + Marketplace 维度的启停状态和接单控制。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `seller_account_id` | bigint | 关联 Seller 配置 |
| `marketplace_id` | varchar(32) | Amazon MarketplaceId |
| `marketplace_name` | varchar(128) | 站点名称 |
| `enabled` | tinyint | 是否启用 |
| `backfill_start_time` | datetime | 首次回补起点 |
| `last_success_sync_time` | datetime | 最近成功同步时间 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_account_marketplace(seller_account_id, marketplace_id)`

### 表 3：`order_amazon_subscription`

用途：

- 管理 SQS destination、`ORDER_CHANGE` subscription 和订阅健康状态。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `seller_account_id` | bigint | 关联 Seller 配置 |
| `queue_arn` | varchar(512) | SQS ARN |
| `queue_url` | varchar(512) | SQS URL |
| `destination_id` | varchar(64) | `createDestination` 返回值 |
| `subscription_id` | varchar(64) | `createSubscription` 返回值 |
| `payload_version` | varchar(16) | 默认 `1.0` |
| `order_change_types_json` | json / text | 订阅的 `OrderStatusChange`、`BuyerRequestedChange` |
| `subscription_status` | varchar(32) | `ACTIVE/INACTIVE/ERROR` |
| `last_verified_time` | datetime | 最近校验时间 |
| `last_error_message` | varchar(1024) | 最近异常原因 |
| `raw_subscription_json` | json / text | Amazon 原始响应快照 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_account_subscription(seller_account_id, subscription_id)`
- 普通索引：`idx_destination(destination_id)`

说明：

- 该表是 Seller 级，不按 Marketplace 拆订阅，因为 `ORDER_CHANGE` 不支持 `marketplaceIds` 过滤。

### 表 4：`order_sync_cursor`

用途：

- 保存 Seller + Marketplace 维度的增量游标和分页状态。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `seller_account_id` | bigint | Seller 配置 |
| `marketplace_id` | varchar(32) | 站点 |
| `cursor_type` | varchar(32) | `BACKFILL/INCREMENTAL/REPAIR` |
| `window_start_time` | datetime | 当前窗口起点 |
| `window_end_time` | datetime | 当前窗口终点 |
| `last_high_watermark` | datetime | 已确认推进的高水位 |
| `pagination_token` | varchar(2048) | `searchOrders` 翻页 token |
| `token_expire_time` | datetime | token 过期时间 |
| `version` | int | 乐观锁版本 |
| `status` | varchar(32) | `IDLE/RUNNING/FAILED` |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_cursor(seller_account_id, marketplace_id, cursor_type)`

### 表 5：`order_sync_task`

用途：

- 保存同步任务、修复任务、详情补全任务。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `task_no` | varchar(64) | 任务号 |
| `seller_account_id` | bigint | Seller 配置 |
| `marketplace_id` | varchar(32) | 站点 |
| `task_type` | varchar(32) | `BACKFILL/INCREMENTAL/REPAIR/DETAIL_ENRICHMENT/NOTIFICATION_DRIVEN` |
| `source_type` | varchar(32) | `SCHEDULE/MANUAL/SQS` |
| `amazon_order_id` | varchar(64) | 单单任务时使用 |
| `window_start_time` | datetime | 时间窗开始 |
| `window_end_time` | datetime | 时间窗结束 |
| `status` | varchar(32) | `PENDING/RUNNING/SUCCESS/FAILED/CANCELLED` |
| `retry_count` | int | 重试次数 |
| `next_retry_time` | datetime | 下次重试时间 |
| `last_error_message` | varchar(1024) | 最近失败原因 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_task_no(task_no)`
- 普通索引：`idx_task_status(status, next_retry_time)`
- 普通索引：`idx_task_order(seller_account_id, marketplace_id, amazon_order_id)`

### 表 6：`order_change_notification`

用途：

- 保存 SQS 通知原文、去重状态和处理结果。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `notification_id` | varchar(64) | `NotificationMetadata.NotificationId` |
| `seller_id` | varchar(64) | 通知中的 SellerId |
| `marketplace_id` | varchar(32) | 通知中的 MarketplaceId |
| `amazon_order_id` | varchar(64) | 通知中的 AmazonOrderId |
| `order_change_type` | varchar(32) | `OrderStatusChange/BuyerRequestedChange` |
| `event_time` | datetime | `EventTime` |
| `publish_time` | datetime | `NotificationMetadata.PublishTime` |
| `sqs_message_id` | varchar(128) | SQS messageId |
| `consume_status` | varchar(32) | `RECEIVED/DUPLICATE/PROCESSED/FAILED` |
| `raw_payload_json` | json / text | 原始通知 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_notification(notification_id)`
- 普通索引：`idx_notify_order(seller_id, marketplace_id, amazon_order_id)`

### 表 7：`order_main`

用途：

- 保存统一订单主记录，不存放敏感地址明文。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `tenant_id` | varchar(64) | 租户标识 |
| `channel_code` | varchar(32) | 固定 `AMAZON_SC` |
| `seller_id` | varchar(64) | SellerId |
| `marketplace_id` | varchar(32) | 站点 |
| `amazon_order_id` | varchar(64) | Amazon 订单号 |
| `sales_channel_name` | varchar(32) | `AMAZON/NON_AMAZON` |
| `order_status` | varchar(32) | Amazon 订单状态 |
| `order_type` | varchar(64) | `StandardOrder` 等 |
| `fulfilled_by_raw` | varchar(32) | `AMAZON/MERCHANT` |
| `fulfillment_mode` | varchar(16) | `FBA/FBM` |
| `fulfillment_status` | varchar(32) | `UNSHIPPED/SHIPPED/...` |
| `purchase_time` | datetime | 下单时间 |
| `created_time_amazon` | datetime | Amazon 创建时间 |
| `last_updated_time_amazon` | datetime | Amazon 更新时间 |
| `ship_by_earliest` / `ship_by_latest` | datetime | 发货窗口 |
| `deliver_by_earliest` / `deliver_by_latest` | datetime | 妥投窗口 |
| `currency_code` | varchar(8) | 主币种 |
| `grand_total_amount` | decimal(18,4) | 订单总额 |
| `buyer_email` | varchar(256) | 买家邮箱 |
| `buyer_company_name` | varchar(256) | 企业买家名称 |
| `programs_json` | json / text | `Amazon Business` 等扩展 program |
| `shipping_programs_json` | json / text | shipping programs |
| `extension_json` | json / text | 其他扩展字段 |
| `data_version` | int | 版本号 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_order_unique(tenant_id, channel_code, seller_id, marketplace_id, amazon_order_id)`
- 普通索引：`idx_order_status(marketplace_id, order_status, last_updated_time_amazon)`

### 表 8：`order_item`

用途：

- 保存订单行。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `order_id` | bigint | 关联主订单 |
| `seller_id` | varchar(64) | SellerId 冗余字段 |
| `marketplace_id` | varchar(32) | Marketplace 冗余字段 |
| `amazon_order_id` | varchar(64) | Amazon 订单号 |
| `order_item_id` | varchar(64) | Amazon 订单行号 |
| `seller_sku` | varchar(128) | Seller SKU |
| `asin` | varchar(32) | ASIN |
| `title` | varchar(512) | 商品标题 |
| `quantity_ordered` | int | 下单数量 |
| `quantity_fulfilled` | int | 已履约数量 |
| `quantity_unfulfilled` | int | 未履约数量 |
| `unit_price_amount` | decimal(18,4) | 单价 |
| `unit_price_currency` | varchar(8) | 币种 |
| `price_designation` | varchar(64) | 如 `BUSINESS_PRICE` |
| `programs_json` | json / text | 行级 program |
| `promotion_json` | json / text | 行级促销 |
| `fulfillment_json` | json / text | 行级履约信息 |
| `extension_json` | json / text | 扩展字段 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_order_item(order_id, order_item_id)`
- 普通索引：`idx_sku(seller_id, marketplace_id, seller_sku)`

### 表 9：`order_pii`

用途：

- 独立保存买家和收件人 PII 明文字段。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `order_id` | bigint | 关联主订单 |
| `buyer_name` | varchar(256) | 买家姓名 |
| `recipient_name` | varchar(256) | 收件人姓名 |
| `recipient_company_name` | varchar(256) | 公司名 |
| `phone` | varchar(64) | 电话 |
| `address_line1` | varchar(256) | 地址行 1 |
| `address_line2` | varchar(256) | 地址行 2 |
| `address_line3` | varchar(256) | 地址行 3 |
| `city` | varchar(128) | 城市 |
| `district_or_county` | varchar(128) | 区县 |
| `state_or_region` | varchar(128) | 州省 |
| `municipality` | varchar(128) | 市政区 |
| `postal_code` | varchar(64) | 邮编 |
| `country_code` | varchar(8) | 国家 |
| `address_type` | varchar(32) | 地址类型 |
| `extended_fields_json` | json / text | 扩展地址字段 |
| `access_level` | varchar(32) | `RESTRICTED` |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_order_pii(order_id)`

说明：

- 应用层默认不直接暴露该表，必须经 PII 应用服务读取。

### 表 10：`order_package`

用途：

- 保存 FBM 包裹与跟踪信息。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `order_id` | bigint | 关联主订单 |
| `package_reference_id` | varchar(128) | 包裹参考号 |
| `package_status` | varchar(64) | 包裹状态 |
| `package_detailed_status` | varchar(64) | 明细状态 |
| `carrier` | varchar(128) | 物流商 |
| `shipping_service` | varchar(128) | 物流服务 |
| `tracking_number` | varchar(128) | 运单号 |
| `ship_time` | datetime | 发货时间 |
| `ship_from_address_json` | json / text | 发货地址 |
| `package_items_json` | json / text | 包裹项 |
| `created_time_amazon` | datetime | Amazon 创建时间 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_package(order_id, package_reference_id)`
- 普通索引：`idx_tracking(tracking_number)`

### 表 11：`order_raw_snapshot`

用途：

- 保存 Amazon 原始响应/通知快照，供审计和问题排查。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `order_id` | bigint | 可为空，订单未落库前可能缺失 |
| `seller_id` | varchar(64) | SellerId |
| `marketplace_id` | varchar(32) | 站点 |
| `amazon_order_id` | varchar(64) | Amazon 订单号 |
| `snapshot_source` | varchar(32) | `ORDER_CHANGE/SEARCH_ORDERS/GET_ORDER` |
| `source_key` | varchar(128) | 如 `notificationId` / `requestId` |
| `payload_hash` | varchar(64) | 内容哈希 |
| `payload_json` | longtext | 原始内容 |
| `created_at` | datetime | 创建时间 |

索引建议：

- 普通索引：`idx_snapshot_order(seller_id, marketplace_id, amazon_order_id, snapshot_source)`

### 表 12：`order_outbox`

用途：

- 保存待发下游事件。

关键字段：

| 字段 | 类型建议 | 说明 |
|------|-----------|------|
| `id` | bigint | 主键 |
| `aggregate_type` | varchar(32) | 固定 `ORDER` |
| `aggregate_id` | bigint | 订单主键 |
| `event_type` | varchar(64) | `ORDER_RECEIVED/ORDER_CHANGED/ORDER_ENRICHED/...` |
| `event_key` | varchar(128) | 幂等键 |
| `payload_json` | longtext | 事件体 |
| `status` | varchar(32) | `PENDING/SENT/FAILED` |
| `retry_count` | int | 重试次数 |
| `next_retry_time` | datetime | 下次重试时间 |
| `created_at` / `updated_at` | datetime | 审计字段 |

索引建议：

- 唯一索引：`uk_event_key(event_key)`
- 普通索引：`idx_outbox_status(status, next_retry_time)`

## 类设计

### `oceanlink-order-start`

- `OceanlinkOrderApplication`
  说明：启动类。
- `OrderModuleAutoConfiguration`
  说明：装配 `adapter/app/domain/infrastructure` 组件。
- `AmazonSpApiProperties`
  说明：Amazon 端点、超时、重试、默认 includedData 配置。
- `AmazonSqsProperties`
  说明：SQS 拉取批量、可见性超时、长轮询秒数、删除策略。
- `OrderSyncSchedulerProperties`
  说明：backfill、reconciliation、repair、outbox relay 调度参数。

### `oceanlink-order-client`

- `AmazonSellerAccountCmd`
  说明：Seller 账户配置命令。
- `AmazonMarketplaceBindCmd`
  说明：站点绑定命令。
- `AmazonSubscriptionBootstrapCmd`
  说明：订阅初始化命令。
- `OrderBackfillCmd`
  说明：回补任务命令。
- `OrderRepairCmd`
  说明：修复任务命令。
- `OrderDTO`
  说明：统一订单对外 DTO。
- `OrderPiiDTO`
  说明：受控 PII DTO。
- `OrderSyncTaskDTO`
  说明：同步任务 DTO。
- `OrderChangedEvent`
  说明：下游订单事件 DTO。

### `oceanlink-order-adapter`

- `AmazonSellerAccountController`
  说明：Seller 账户配置入口。
- `AmazonSubscriptionController`
  说明：destination / subscription 管理入口。
- `OrderSyncController`
  说明：backfill / repair / cursor 查询入口。
- `OrderQueryController`
  说明：订单和 PII 查询入口。
- `SqsOrderChangeAdapter`
  说明：SQS 消息接收适配器，负责把外部消息转成应用命令。
- `BackfillSyncScheduler`
  说明：回补任务调度入口。
- `IncrementalReconciliationScheduler`
  说明：轮询补偿调度入口。
- `RepairSyncScheduler`
  说明：修复任务调度入口。
- `OrderDtoAssembler`
  说明：adapter 与 client DTO 之间的装配器。

### `oceanlink-order-app`

- `AmazonSellerAccountCmdExe`
  说明：处理 Seller 账户创建/更新。
- `AmazonMarketplaceBindCmdExe`
  说明：处理 Marketplace 绑定。
- `AmazonSubscriptionBootstrapCmdExe`
  说明：处理 destination / subscription 创建与校验。
- `OrderNotificationCmdExe`
  说明：处理 `ORDER_CHANGE` 消息去重、路由和任务生成。
- `OrderBackfillCmdExe`
  说明：处理 backfill 任务编排。
- `OrderRepairCmdExe`
  说明：处理 repair 任务编排。
- `OrderFetchCmdExe`
  说明：根据任务类型触发 `searchOrders` 或 `getOrder`。
- `OrderPersistCmdExe`
  说明：处理订单聚合落库、快照落库和 outbox。
- `OrderQueryExe`
  说明：查询标准化订单。
- `OrderPiiQueryExe`
  说明：受控查询 PII 并记审计。

### `oceanlink-order-domain`

- `OrderAggregate`
  说明：订单主聚合，含订单头、订单行、扩展字段、版本控制。
- `OrderPiiAggregate`
  说明：PII 聚合，控制更新条件和访问级别。
- `OrderSyncCursor`
  说明：窗口推进、token 续用、失败回滚规则。
- `OrderSyncTask`
  说明：任务状态机。
- `OrderChangeNotification`
  说明：通知领域对象，包含去重键和路由键。
- `OrderMergePolicy`
  说明：新旧订单数据合并规则。
- `OrderEventFactory`
  说明：统一生成下游领域事件。
- `OrderRepository`
  说明：订单聚合仓储接口。
- `OrderPiiRepository`
  说明：PII 仓储接口。
- `OrderSyncTaskRepository`
  说明：同步任务仓储接口。
- `AmazonOrdersGateway`
  说明：Orders API 访问网关接口。
- `AmazonNotificationsGateway`
  说明：Notifications API 访问网关接口。
- `OrderEventGateway`
  说明：Outbox / 下游事件网关接口。

### `oceanlink-order-infrastructure`

- `AmazonOrdersGatewayImpl`
  说明：封装 `searchOrders`、`getOrder`。
- `AmazonNotificationsGatewayImpl`
  说明：封装 `createDestination`、`createSubscription`、`getSubscription`。
- `AmazonLwaTokenGatewayImpl`
  说明：刷新访问令牌并缓存短期 token。
- `AmazonOrderPayloadMapper`
  说明：Amazon DTO -> 内部标准 DTO 映射。
- `OrderRepositoryImpl`
  说明：订单仓储实现。
- `OrderPiiRepositoryImpl`
  说明：PII 仓储实现。
- `OrderSyncTaskRepositoryImpl`
  说明：同步任务仓储实现。
- `OrderEventGatewayImpl`
  说明：Outbox 事件落库与投递实现。
- `AmazonSellerAccountMapper`
- `AmazonMarketplaceBindingMapper`
- `AmazonSubscriptionMapper`
- `OrderSyncCursorMapper`
- `OrderSyncTaskMapper`
- `OrderChangeNotificationMapper`
- `OrderMainMapper`
- `OrderItemMapper`
- `OrderPiiMapper`
- `OrderPackageMapper`
- `OrderRawSnapshotMapper`
- `OrderOutboxMapper`

实现约束：

- Amazon 侧请求/响应 DTO 命名和字段优先对齐 `selling-partner-api-models` 中的 schema。
- 仅在进入 `domain` 层时才转换为 OceanLink 内部模型，避免把手工简化 DTO 直接暴露给领域层。

## 模块设计细化

### 1. 账户与站点管理

职责：

- 管理 Seller 级授权。
- 管理 Seller + Marketplace 绑定。
- 校验接入前置条件。

设计要点：

- Seller 级保存刷新令牌和区域信息。
- Marketplace 级保存启停状态和回补起点。
- 初始化时不自动为每个 Marketplace 创建单独订阅。
- `adapter` 接收管理命令，`app` 负责编排，`infrastructure` 负责 Amazon 校验和持久化。

### 2. 通知消费模块

职责：

- 轮询 SQS。
- 去重。
- 路由。
- 生成详情拉取任务。

设计要点：

- 去重键优先使用 `notificationId`。
- 站点路由键使用 `sellerId + marketplaceId`。
- SQS 消费成功标准不是“拿到消息”，而是“任务已落库并可重试”。
- SQS 属于入站适配器，因此消费逻辑入口放在 `adapter`，而不是 `infrastructure`。

### 3. 订单抓取模块

职责：

- 执行 `getOrder` 和 `searchOrders`。
- 进行 Amazon 限流保护和失败重试。

设计要点：

- `NOTIFICATION_DRIVEN` 默认直接调用 `getOrder`。
- `BACKFILL/REPAIR/INCREMENTAL` 默认调用 `searchOrders`。
- 若 `searchOrders` 返回信息不足，再创建 `DETAIL_ENRICHMENT` 调用 `getOrder`。
- Amazon API 属于出站依赖，因此客户端和重试逻辑放在 `infrastructure`，通过 `domain gateway` 暴露给 `app`。

### 4. 标准化入库模块

职责：

- 标准化订单对象。
- 合并写库。
- 管理 PII 分表。
- 写原始快照和 outbox。

设计要点：

- 订单主表与 PII 表同事务写入。
- 程序扩展字段统一收敛到 JSON。
- 保留原始 `FulfilledBy` 和业务映射后的 `FBA/FBM`。
- `app` 控制事务边界，`domain` 控制合并规则，`infrastructure` 负责仓储实现。

### 5. 可观测与修复模块

职责：

- 管理同步任务。
- 提供状态查询。
- 支撑人工 repair。

设计要点：

- repair 支持按时间窗和单订单号重放。
- cursor 推进需要乐观锁，避免并发任务踩踏。
- 订阅状态和队列健康度需要定时校验。
- 调度入口在 `adapter`，任务流转和状态推进在 `app`。

## 接口设计

### Amazon 外部接口封装

#### `AmazonOrdersClient`

方法：

- `searchOrders(SearchOrdersCommand command)`
- `getOrder(GetOrderCommand command)`

实现规则：

- 自动注入 Seller 对应 access token。
- 记录 `x-amzn-RequestId`、`x-amzn-RateLimit-Limit`。
- 对 `429/500/503` 做可配置重试。
- 请求参数和响应 DTO 优先按 `orders_2026-01-01.json` 建模，不自创字段名。

#### `AmazonNotificationsClient`

方法：

- `createDestination(CreateSqsDestinationCommand command)`
- `createOrderChangeSubscription(CreateOrderChangeSubscriptionCommand command)`
- `getOrderChangeSubscription(GetSubscriptionCommand command)`

实现规则：

- `createDestination` 作为 grantless 调用处理。
- `createSubscription` 绑定 Seller 授权上下文。
- `processingDirective` 默认订阅全部 `orderChangeTypes`，也允许按配置只订阅 `OrderStatusChange`。
- destination 和 subscription DTO 按 `notifications.json` 中的 `CreateDestinationRequest`、`CreateSubscriptionRequest`、`Subscription` 结构建模。

### 模块内部管理接口

#### 账户与绑定

- `POST /admin/order/amazon/accounts`
  说明：创建或更新 Seller 账户配置。
- `POST /admin/order/amazon/accounts/{accountId}/marketplaces`
  说明：批量绑定 Marketplace。

#### 订阅管理

- `POST /admin/order/amazon/subscriptions/bootstrap`
  说明：创建或校验 destination + subscription。
- `GET /admin/order/amazon/subscriptions/{accountId}`
  说明：查询订阅状态。

#### 同步管理

- `POST /admin/order/amazon/sync/backfill`
  说明：提交 backfill 任务。
- `POST /admin/order/amazon/sync/repair`
  说明：提交 repair 任务。
- `GET /admin/order/amazon/sync/cursors`
  说明：查询各 Marketplace 游标。
- `GET /admin/order/amazon/sync/tasks/{taskNo}`
  说明：查询任务状态。

#### 查询接口

- `GET /admin/order/orders/{amazonOrderId}`
  说明：查询标准化订单详情。
- `GET /admin/order/orders/{amazonOrderId}/pii`
  说明：受控读取 PII，必须记审计。

### 下游事件接口

事件主题建议：

- `order.amazon.standardized.v1`
- `order.amazon.changed.v1`
- `order.amazon.enriched.v1`

事件公共字段：

- `eventId`
- `eventType`
- `tenantId`
- `sellerId`
- `marketplaceId`
- `amazonOrderId`
- `orderId`
- `sourceType`
- `occurredAt`
- `payload`

## 数据合并与幂等策略

### 订单幂等键

- 主订单：`tenantId + channelCode + sellerId + marketplaceId + amazonOrderId`
- 订单行：`orderId + orderItemId`
- 通知：`notificationId`
- outbox：`eventType + orderId + dataVersion`

### 字段覆盖规则

- 状态类字段以 `lastUpdatedTimeAmazon` 更新较新的记录覆盖较旧记录。
- 非空增强字段可以覆盖旧的空值，但不能被旧快照回写为更老状态。
- PII 字段仅在本次响应包含对应数据块时更新。
- 包裹信息仅在 `PACKAGES` 存在时更新，不主动删除旧包裹，删除场景以后续显式规则补充。

### Cursor 推进规则

- 只有当前窗口页全部成功持久化后才推进 `last_high_watermark`。
- 若 `paginationToken` 未处理完，不推进高水位。
- `paginationToken` 过期后，以当前窗口起点重新抓取并依赖幂等写库去重。

## TDD 实施顺序

### 第 1 批：纯领域与映射测试

- `OrderFulfillmentModeMapperTest`
- `OrderMergePolicyTest`
- `OrderNotificationDedupKeyTest`
- `OrderProgramsExtensionMapperTest`
- `OrderPiiAccessPolicyTest`

### 第 2 批：应用服务测试

- `OrderNotificationApplicationServiceTest`
- `OrderSyncTaskApplicationServiceTest`
- `OrderFetchApplicationServiceTest`
- `OrderPersistenceApplicationServiceTest`

### 第 3 批：集成与适配测试

- `AmazonOrdersClientIntegrationTest`
- `AmazonNotificationsClientIntegrationTest`
- `SqsOrderChangeConsumerIntegrationTest`
- `OrderOutboxRelayIntegrationTest`

### 第 4 批：端到端场景测试

- `AmazonOrderBackfillE2ETest`
- `AmazonOrderNotificationDrivenSyncE2ETest`
- `AmazonOrderRepairSyncE2ETest`

## 单元测试设计

重点覆盖：

- `FulfilledBy` -> `FBA/FBM` 映射。
- `ORDER_CHANGE` payload 解析和 `notificationId` 去重。
- 订单新旧快照合并。
- `paginationToken` 过期回放策略。
- PII 可读权限和审计记录触发。
- `Amazon Business` 等 program 扩展字段的 JSON 映射。

Mock 边界：

- Amazon API 客户端全部通过接口 mock。
- Repository 使用内存桩或 MockBean。
- 时钟使用可注入 `Clock`。

## 集成测试设计

重点场景：

- `createDestination` grantless 调用链。
- `createSubscription(ORDER_CHANGE)` 调用链。
- SQS 重复消息多次投递只生成一次有效任务。
- 通知先到、`getOrder` 暂时查不到、稍后重试成功。
- `searchOrders` 多页回补 + token 续跑。
- 同一订单被通知和 repair 同时命中时的幂等写库。
- PII 明文落库后，非授权查询接口不可直接返回。
- outbox 发送失败重试不丢事件。

环境建议：

- 数据库使用 Testcontainers MySQL。
- SQS 可使用 LocalStack 或本地兼容实现。
- Amazon API 用 WireMock 录制标准响应，并优先基于 `selling-partner-api-models` 与官方示例构造 contract fixture。

## 实施顺序

1. 建立模块骨架、`pom.xml`、启动类、测试基建。
2. 建立 COLA 子模块：`client / domain / app / infrastructure / adapter / start`。
3. 完成 DDL、DO、Mapper 和 Repository/Gateway 接口骨架。
4. 先写通知解析、去重、订单聚合合并的单元测试。
5. 再写 `getOrder` 驱动的通知接单闭环。
6. 补 `searchOrders` backfill / repair 闭环。
7. 最后实现 outbox、管理接口、可观测和审计。

## 风险与预案

- `searchOrders` 限流过低：
  预案：仅用于 backfill / reconciliation，不承担实时主链路。
- `paginationToken` 过期：
  预案：从窗口起点重放，依赖幂等写库。
- SQS 重复/乱序：
  预案：`notificationId` 去重 + 订单版本合并。
- PII 明文落库带来误用风险：
  预案：分表、独立服务、访问审计。
- 任意 Marketplace 扩展导致站点规则不一致：
  预案：站点能力放到配置和扩展字段，不写死分支。

## 参考资料

- Selling Partner API Models 仓库：https://github.com/amzn/selling-partner-api-models
- Orders API Migration Guide：https://developer-docs.amazon.com/sp-api/lang-US/docs/orders-api-migration-guide
- Orders API v2026-01-01 model：https://raw.githubusercontent.com/amzn/selling-partner-api-models/main/models/orders-api-model/orders_2026-01-01.json
- Notifications API model：https://raw.githubusercontent.com/amzn/selling-partner-api-models/main/models/notifications-api-model/notifications.json
- Java auth/client templates：https://github.com/amzn/selling-partner-api-models/tree/main/clients/sellingpartner-api-aa-java
- Orders API rate limits：https://developer-docs.amazon.com/sp-api/docs/orders-api-rate-limits
- Orders API changelog：https://developer-docs.amazon.com/sp-api/lang-zh_CN/changelog/new-introducing-the-orders-api-v2026-01-01
- Subscribe to the `ORDER_CHANGE` notification：https://developer-docs.amazon.com/sp-api/docs/tutorial-subscribe-to-order-change-notification
- Set up notifications using Amazon SQS：https://developer-docs.amazon.com/sp-api/lang-ja_JP/docs/set-up-notifications-with-amazon-sqs
- Grant the SP-API permission to an Amazon SQS queue：https://developer-docs.amazon.com/sp-api/lang-US/docs/tutorial-grant-permission-to-sqs-queue
- Access Orders PII：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/access-orders-pii
