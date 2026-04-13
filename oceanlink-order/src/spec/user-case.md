# Amazon SC 接单模块用户场景

## 文档目标

本文档用于定义 `oceanlink-order` 首期能力的产品级目标、用户场景、边界和业务价值。当前仅覆盖 Amazon Seller Central（SC）接单模块，后续 `task.md` 和 `plan.md` 将在本文件评审通过后继续展开。

## 产品背景

OceanLink 当前缺少统一订单域入口，无法稳定承接 Amazon 订单并把订单状态、金额、履约信息同步给库存、履约、财务等下游模块。Amazon SP-API 的 Orders API 已提供订单查询能力，Notifications API 提供 `ORDER_CHANGE` 事件，可作为增量接单触发器；结合 Amazon 官方通知工作流，首期通知链路明确使用 Amazon SQS 标准队列承接消息，因此首期以 Amazon SC 接单作为 `oceanlink-order` 的切入点。

## 目标价值

- 建立 OceanLink 的统一订单接入入口，避免各业务系统分别直连 Amazon。
- 支持任意 `marketplaceId` 的 Amazon 多站点订单接入，为库存、履约、财务提供标准化订单主数据。
- 通过“SQS 通知驱动 + 轮询补偿”的方式降低 Orders API 调用压力，减少漏单风险。
- 对买家敏感信息建立明文落库前提下的权限隔离、访问审计和最小化暴露机制，满足 Amazon 受限数据访问要求。
- 为后续扩展 Walmart、Shopify、独立站等渠道订单接入预留统一订单模型。

## 目标角色

- 平台运营：负责授权 Seller 账号、选择站点、观察接单状态和补单结果。
- 订单集成服务：负责调用 Amazon 接口、消费通知、维护增量游标和回补任务。
- 下游业务模块：消费统一订单事件，完成库存预占、履约建单、财务记账和报表统计。

## 首期范围

首期必须完成的产品能力：

- 接入 Amazon Orders API `v2026-01-01` 的 `searchOrders` 和 `getOrder`。
- 接入 Amazon Notifications API 的 `ORDER_CHANGE` 通知，并使用 Amazon SQS 标准队列承接消息。
- 支持按 Seller、按站点维护订单接入配置和增量水位。
- 支持历史订单回补、日常增量接单、异常补单修复三类接单模式。
- 支持 `createDestination` / `createSubscription` 对应的通知接入配置管理，至少覆盖队列 ARN、`destinationId`、订阅状态和事件过滤条件。
- 支持订单主信息、订单行、金额、促销、履约信息的标准化存储。
- 支持基于 Amazon 订单号的幂等入库和重复通知去重。
- 支持 Amazon 受限数据的最小化拉取、明文落库、权限隔离和访问审计。

## 非目标范围

首期明确不做的内容：

- 发货确认、取消确认、退货退款和售后流程。
- Amazon 多渠道订单（MCF）履约出库。
- 非 Amazon 渠道订单接入。
- 复杂订单编排、拆单、合单、规则引擎。
- 风控、客服消息、营销活动等订单外围能力。

## 核心用户场景

### 场景 1：Seller 首次接入 Amazon 订单

- 触发条件：平台新绑定一个 Amazon Seller 账号，并选择若干 `marketplaceIds`。
- 用户诉求：快速验证授权有效性，并把最近一段时间的历史订单回补到 OceanLink。
- 期望结果：系统完成初次 backfill，建立每个站点的增量水位，并可看到接单成功/失败统计。

### 场景 2：日常增量接单

- 触发条件：Amazon 产生新订单或订单状态发生变化。
- 用户诉求：订单能尽量实时进入 OceanLink，不依赖高频轮询。
- 期望结果：SQS 中的 `ORDER_CHANGE` 通知优先触发增量处理，轮询任务负责兜底补偿，避免漏单。

### 场景 3：订单详情补全

- 触发条件：拉单结果只拿到部分字段，或者某些业务链路需要更完整的履约、买家、包裹、费用信息。
- 用户诉求：系统能够按需补拉订单详情，而不是对所有订单都获取全量敏感数据。
- 期望结果：按 `includedData` 精准拉取所需块数据，并根据权限决定是否允许获取 `buyer`、`recipient` 等敏感信息。

### 场景 4：异常补单与数据修复

- 触发条件：SQS 通知丢失、轮询任务失败、下游写库异常，或运营怀疑某时间段漏单。
- 用户诉求：可以按时间窗口、Seller、站点、订单号执行定向补单。
- 期望结果：系统支持回放与幂等修复，不因重复拉单造成脏数据或重复业务事件。

### 场景 5：多站点统一接单

- 触发条件：同一 Seller 或同一租户下存在多个 Amazon 站点。
- 用户诉求：统一观察不同站点的接单情况，同时保持站点间水位、授权、币种、统计口径隔离。
- 期望结果：系统按 `marketplaceId` 管理游标与指标，并向下游输出统一订单模型。

### 场景 6：受限数据最小化处理

- 触发条件：客服、履约等下游确实需要买家姓名、地址、电话等敏感字段。
- 用户诉求：在满足角色权限的前提下获取必要数据，同时避免无权限链路接触敏感信息。
- 期望结果：系统对受限字段建立独立授权、可控明文落库、访问审计和受限访问策略。

## 业务约束

- `searchOrders` 需要按时间条件和 `marketplaceIds` 查询，返回结果存在翻页和 `nextToken`。
- Orders API 的查询频率较低，首期设计必须采用“SQS 通知优先、轮询兜底”的混合模式。
- `ORDER_CHANGE + SQS` 是首期强依赖能力；若通知主链路未打通，则首期能力不视为完成。
- `ORDER_CHANGE` 通知不仅覆盖订单状态变化，也覆盖买家请求修改等事件，需要支持重复、乱序和延迟处理。
- `ORDER_CHANGE` 订阅的 `processingDirective` 不支持 `marketplaceIds` 过滤，因此站点隔离必须在消息消费后按通知载荷中的 `MarketplaceId` 处理。
- SP-API 通知当前要求接入 Amazon SQS 标准队列，不支持 FIFO 队列；因此消费端必须接受近似有序、重复投递和至少一次投递语义。
- 若 SQS 队列启用服务端加密（SSE/KMS），还需要为 SP-API 的 AWS Principal 授予 KMS 访问权限。
- Amazon 订单既可能由 `AMAZON` 履约，也可能由 `MERCHANT` 履约；业务上可分别映射为 `FBA` 和 `FBM`，但统一模型需要保留原始 `FulfilledBy` 值。
- `Orders API v2026-01-01` 可通过 `includedData` 控制返回块，敏感数据访问依赖 Amazon 受限角色。
- 买家姓名、电话、地址已确认允许明文落库，但必须附带授权边界、访问审计和字段级最小暴露策略。
- 多渠道订单（MCF）虽然是 Amazon 相关术语，但不属于首期接单范围，后续应单独作为履约型能力设计。

## 成功判定

- 运营能够完成 Seller 授权后的历史订单回补，并查看按站点的接单状态。
- 系统能够稳定接收 Amazon 新订单和状态变更，不因 SQS 重复投递或乱序消息产生重复订单数据。
- 下游模块能够消费统一订单事件，而不需要直接理解 Amazon 原始字段结构。
- 敏感字段获取路径清晰，默认最小化暴露，满足权限隔离要求。

## 已确认评审结论

- 首期按任意 `marketplaceId` 设计，不限制为美国站。
- 首期允许买家姓名、电话、地址明文落库，但必须满足授权边界和访问审计要求。
- 首期必须接通 `ORDER_CHANGE + SQS`，不接受仅靠轮询的最小闭环。
- `Amazon Business` 等 program 信息首期先作为扩展字段保留。

## 参考资料

- Orders API：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/orders-api
- Get orders with filtering criteria：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/get-orders-with-filtering-criteria
- Orders API model (`2026-01-01`)：https://github.com/amzn/selling-partner-api-models/blob/main/models/orders-api-model/orders_2026-01-01.json
- Subscribe to the `ORDER_CHANGE` notification：https://developer-docs.amazon.com/sp-api/docs/tutorial-subscribe-to-order-change-notification
- Set up notifications using Amazon SQS：https://developer-docs.amazon.com/sp-api/lang-ja_JP/docs/set-up-notifications-with-amazon-sqs
- Grant the SP-API permission to an Amazon SQS queue：https://developer-docs.amazon.com/sp-api/lang-US/docs/tutorial-grant-permission-to-sqs-queue
- Access Orders PII：https://developer-docs.amazon.com/sp-api/lang-zh_CN/docs/access-orders-pii
- Orders API rate limits：https://developer-docs.amazon.com/sp-api/docs/orders-api-rate-limits
- Fulfillment Outbound API reference：https://developer-docs.amazon.com/sp-api/docs/fulfillment-outbound-api-v2020-07-01-reference
