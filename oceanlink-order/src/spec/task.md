# Amazon SC 接单模块开发任务

## 文档目标

本文档基于已评审通过的 `user-case.md`，将 Amazon Seller Central（SC）接单模块拆解为可执行的开发任务、边界条件和测试任务。本文档只定义“做什么”和“做到什么程度”，不展开 DDL、类图、接口明细和测试用例实现细节，这些内容留到 `plan.md`。

## 已确认输入

- 首期对接 Amazon Orders API `v2026-01-01`，核心操作为 `searchOrders` 和 `getOrder`。
- 首期通知链路必须打通 `ORDER_CHANGE + Amazon SQS Standard Queue`。
- Orders API 轮询只作为通知缺失、延迟和补单修复时的兜底手段。
- 首期按任意 `marketplaceId` 设计，不能把站点能力限定为美国站。
- 买家姓名、电话、地址允许明文落库，但必须建立授权边界、访问审计和最小暴露策略。
- `Amazon Business`、`Prime`、`Subscribe and Save` 等 program 信息首期保留为扩展字段。
- `FulfilledBy=AMAZON` 可映射为 `FBA`，`FulfilledBy=MERCHANT` 可映射为 `FBM`，同时保留 Amazon 原始值。

## 任务拆分

### 任务 1：模块骨架与工程接入

目标：

- 建立基于 COLA 5.0 的 `oceanlink-order` 聚合模块结构。
- 接入根 `pom.xml` 的模块声明与基础依赖。
- 创建 `client / app / domain / infrastructure / adapter / start` 六个子模块。
- 预留各子模块的 `src/main`、`src/test`、`src/spec` 目录和父模块文档目录。

完成标准：

- `oceanlink-order` 作为父模块可被根工程识别和构建。
- 六个 COLA 子模块的职责边界清晰，依赖方向符合 `adapter -> app -> domain <- infrastructure <- start` 的装配关系。
- `start` 模块作为最终启动模块，`adapter` 负责入站适配，`infrastructure` 负责出站实现。
- README 与 spec 文档链接保持一致。

### 任务 2：Seller 与 Marketplace 接入配置

目标：

- 定义 Seller 授权信息、刷新令牌、应用凭证、站点范围等配置模型。
- 支持 Seller + Marketplace 维度的启停、状态查询和配置校验。
- 为历史回补、增量拉单、通知订阅、补单任务提供统一配置来源。

完成标准：

- 能区分同一 Seller 下不同 `marketplaceId` 的接单状态与水位。
- 能识别无效 marketplace、授权缺失、刷新令牌失效等前置错误。
- 配置模型能支撑首期任意 `marketplaceId` 扩展。

### 任务 3：Notifications + SQS 主链路

目标：

- 封装 Notifications API 的 destination 和 subscription 管理能力。
- 明确 SQS 队列 ARN、`destinationId`、订阅状态、事件过滤条件的管理方式。
- 明确 `ORDER_CHANGE` 不支持 `marketplaceIds` 订阅过滤，站点路由在消息消费阶段完成。
- 建立 SQS 消费链路，用于接收 `ORDER_CHANGE` 消息并驱动订单增量处理。

完成标准：

- 能记录或创建 `ORDER_CHANGE` 所需的 destination / subscription 元数据。
- 能基于通知载荷中的 `SellerId`、`MarketplaceId`、`AmazonOrderId` 做站点级路由和任务投递。
- SQS 消费端支持消息拉取、确认、失败重试和死信处理策略。
- 消费端对重复消息、近似乱序消息和延迟消息具备幂等处理能力。
- 若启用 SSE/KMS，系统能够说明并校验所需权限前置条件。

### 任务 4：Orders API 增量拉单与回补

目标：

- 设计基于 `lastUpdatedAfter` / `lastUpdatedBefore` 的增量拉单窗口。
- 管理 Seller + Marketplace 维度的水位、回补窗口和补单任务。
- 处理 `searchOrders` 的分页、`nextToken`、限流和重试策略。

完成标准：

- 支持首次 backfill、日常增量、指定时间窗补单三类任务模式。
- 支持通知触发后按订单号或时间窗进行增量补抓。
- 支持 Orders API 调用失败后的重试和断点续跑。
- 水位推进不能因为单次失败、空页、重复页造成跳跃或漏单。

### 任务 5：订单详情补全与块数据按需拉取

目标：

- 设计何时只使用 `searchOrders` 结果，何时追加 `getOrder`。
- 基于 `includedData` 控制买家、收件人、包裹、促销、收益等块数据的补全策略。
- 对通知驱动场景支持“先接住，再补全”的异步明细拉取流程。

完成标准：

- 能区分最小接单字段与增强字段，避免对所有订单无差别拉取详情。
- PII 字段、包裹字段、program 字段可按授权和业务需要补拉。
- 详情补全失败不阻塞基础订单入库，但必须可重试、可审计。

### 任务 6：统一订单模型与持久化

目标：

- 设计统一订单头、订单行、金额、地址、买家、配送、履约、扩展字段模型。
- 设计 Amazon 原始字段与统一领域字段的映射规则。
- 明确哪些字段属于固定列，哪些字段以扩展字段或原始快照形式保存。

完成标准：

- 能保留 Amazon 原始 `amazonOrderId`、`marketplaceId`、`FulfilledBy`、订单状态等关键字段。
- `Amazon Business` 等 program 信息以扩展字段存储，不阻塞后续扩展为一等字段。
- PII 明文字段与普通字段具备明确的存储边界和访问边界。
- 同一订单多次接收时支持幂等更新，而不是重复插入。

### 任务 7：幂等、去重与状态演进

目标：

- 设计订单主键、自然键、消息去重键和更新版本策略。
- 支持重复通知、重复拉单、不同时间返回不同完整度数据时的合并更新。
- 设计订单状态更新时的字段覆盖规则，避免被旧数据回写。

完成标准：

- 同一 `amazonOrderId` 在相同 Seller + Marketplace 维度下只能存在一份主订单数据。
- SQS 重复消息不会导致重复业务事件或重复写库。
- 新旧数据合并时，状态、水位、时间戳、详情补全结果具备可解释的覆盖策略。

### 任务 8：PII 权限、明文落库与访问审计

目标：

- 建立受限字段拉取权限、明文落库权限和读取权限边界。
- 明确哪些字段允许明文持久化，哪些链路只能读取脱敏结果。
- 设计访问审计、变更审计和异常访问告警要求。

完成标准：

- 买家姓名、电话、地址可明文存储，但必须有字段级访问控制或独立访问门面。
- 无受权链路不能直接读取明文字段。
- PII 相关操作具备审计事件或审计日志。

### 任务 9：下游事件输出

目标：

- 设计订单入库后向库存、履约、财务等下游发送的统一事件。
- 区分“新单接入”“订单变更”“详情补全完成”“补单修复完成”等事件类型。
- 约束事件发送与主库更新之间的一致性策略。

完成标准：

- 下游不需要依赖 Amazon 原始消息结构即可消费订单事件。
- 事件重复发送、顺序错乱、补单回放不会破坏下游幂等性。
- 事件内容能标识 Seller、Marketplace、订单主键、变更来源和变更时间。

### 任务 10：运维与可观测能力

目标：

- 提供接单状态、通知消费状态、API 调用状态、补单状态的可观测信息。
- 设计告警阈值，例如授权失败、SQS 堆积、通知长期缺失、API 限流、补单失败。
- 提供人工排查所需的诊断信息。

完成标准：

- 能按 Seller + Marketplace 查看接单健康度。
- 能定位通知未消费、消息反复失败、订单长期未补全等问题。
- 能输出水位、重试次数、失败原因、最近成功时间等关键指标。

## 边界条件

- 同一 Seller 可能同时接入多个 `marketplaceId`，不同站点的游标、通知订阅状态和统计指标不能混用。
- `ORDER_CHANGE` 订阅阶段不能按 `marketplaceIds` 过滤，因此站点隔离不能依赖 Amazon 订阅配置。
- `ORDER_CHANGE` 通知与 Orders API 查询结果可能存在时间差，通知到达时订单详情未必已可查询，需支持延迟补拉。
- SQS 标准队列不保证严格顺序，消费端不能假设消息天然有序。
- 订单多次返回时字段完整度可能不同，必须支持“先粗后细”的增量补全。
- 历史回补任务与通知实时任务可能并发命中同一订单，必须保证写入幂等。
- Amazon API 存在限流，不能把所有通知都同步转换为即时全量详情查询。
- PII 虽允许明文落库，但不能因此放弃最小获取和访问审计。
- `Amazon Business` 等 program 首期作为扩展字段保存，后续若提升为固定字段，现有存储结构应可兼容迁移。

## 测试任务

### 单元测试

- Orders API 查询条件构造、分页推进和限流重试逻辑。
- SQS 消息解析、去重键生成和幂等消费逻辑。
- `FulfilledBy` 到 `FBA/FBM` 的映射逻辑及原始值保留逻辑。
- program 扩展字段映射逻辑。
- PII 字段访问控制和脱敏展示逻辑。

### 集成测试

- `ORDER_CHANGE + SQS` 主链路打通测试。
- destination / subscription 元数据管理测试。
- 首次 backfill 与日常增量任务协同测试。
- 通知先到、详情后到场景测试。
- 重复消息、乱序消息、补单回放场景测试。
- PII 明文落库和受限访问审计测试。

### 回归测试关注点

- 任意 `marketplaceId` 的兼容性，不能把站点逻辑写死为固定枚举。
- 轮询兜底不能绕过通知主链路的状态控制。
- Amazon Business 等扩展字段不能污染核心订单模型。
- 通知失败或限流场景下不能造成水位错误推进。

## 交付判定

- `ORDER_CHANGE + SQS` 主链路可用，并能驱动订单接入。
- 支持任意 `marketplaceId` 的 Seller + Marketplace 维度接单。
- Amazon 订单可以标准化入库，并支持重复通知下的幂等更新。
- 买家姓名、电话、地址可明文落库，同时具备权限控制和访问审计。
- `Amazon Business` 等 program 信息已按扩展字段保存。

## 下一步说明

`task.md` 评审通过后，再进入 `plan.md`，展开以下内容：

- DDL 设计
- 类设计
- 模块设计
- 接口设计
- 单元测试设计
- 集成测试设计
